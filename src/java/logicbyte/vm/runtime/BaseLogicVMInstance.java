package logicbyte.vm.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.loader.DynamicLibraryFile;
import logicbyte.loader.DynamicLibraryFile.DynamicLibraryException;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMFunctionPointer;
import logicbyte.vm.VMGlobalsTable.GlobalBox;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMJFunction;
import logicbyte.vm.VMType;
import logicbyte.vm.exception.DuplicateTypeNameException;
import logicbyte.vm.exception.LinkerNotLoadedException;
import logicbyte.vm.exception.NoSuchFunctionException;
import logicbyte.vm.exception.UnsatisfiedImportException;
import logicbyte.vm.exception.VMException;
import logicbyte.vm.factory.VMClosureFactory;
import logicbyte.vm.factory.VMTypeFactory;
import logictype.BiDiMap;
import logictype.HashMapList;
import logictype.Log;
import logictype.Pair;

/**
 * <p>
 * The virtual machine instance; contains the loaded linker declarations, the loaded bytecode singletons and their associated
 * bytecode-specific closures. The virtual machine instance may generate many callable function references, but only one callable function
 * may execute concurrently; threaded calls to callable function references shall await the virtual machine's internal lock before executing
 * the invoked callable function.</p>
 *
 * <p>
 * The callable function references may be invoked in any specific order; that is, the advance method on one callable function may be
 * invoked followed by subsequent calls to the same advance method, or to any other callable function's advance method.</p>
 *
 * <p>
 * The virtual machine instance contains only singletons of the globals, types, linker data and bytecode data; that is, for each virtual
 * machine instance a new global and types table will be created. The linker and bytecode data is not copied and is shared between multiple
 * virtual machines.</p>
 *
 * <p>
 * Loaded linker and bytecode data references should be considered immutable.</p>
 *
 * @author Lochie
 */
public class BaseLogicVMInstance implements LogicVM {

    /**
     * The logger default destination (as assigned by {@link #setLogger(logictype.Log)}), or the void logger
     */
    protected Log logger = Log.VoidLog.INSTANCE;

    /**
     * The loader reference which shall be used to find and load linkers / bytecode data
     */
    final DynamicLibraryFile loaderImplObj;

    /**
     * The factory which will be used to initialize closures
     */
    final VMClosureFactory closureFactory;

    /**
     * The factory which will be used to initialize types
     */
    final VMTypeFactory typeFactory;

    /**
     * The global type hierarchy index
     */
    final BaseVMTypeIndex globalTypesIndex;

    /**
     * The bi-directional map of all loaded linker &amp; bytecode objects
     */
    final BiDiMap< LinkerObjectFile, LogicBytecodeFile> loadedObjMap;

    /**
     * The map of all linkers to closures
     */
    final Map<LinkerObjectFile, VMClosure> closureMap;

    /**
     * The map of dependencies
     */
    final HashMapList<LinkerObjectFile, LinkerObjectFile> dependencyMap;

    /**
     * The VM global map of all Java callable functions
     */
    final BiDiMap<String, VMJFunction> jfuncTable;

    /**
     * The global execution lock
     */
    final Object globalLock = new Object();

    /**
     * Default constructor.
     *
     * @param loaderObj      The dynamic loader which shall be used to locate linker and bytecode file(s).
     * @param closureFactory The closure factory
     * @param typeFactory    The type factory
     */
    public BaseLogicVMInstance(DynamicLibraryFile loaderObj, VMClosureFactory closureFactory, VMTypeFactory typeFactory) {
	this.loaderImplObj = loaderObj;
	this.closureFactory = closureFactory;
	this.typeFactory = typeFactory;
	this.globalTypesIndex = new BaseVMTypeIndex();
	this.loadedObjMap = new BiDiMap<>();
	this.closureMap = new HashMap<>();
	this.dependencyMap = new HashMapList<>();
	this.jfuncTable = new BiDiMap<>();
    }

    @Override
    public void setLogger(Log receiver) {
	this.logger = receiver;
    }

    @Override
    public VMClosure load(final VMDebugCallback callback, final LinkerObjectFile linker) throws InterruptedException, DynamicLibraryException, VMException, UnsatisfiedImportException {
	if (linker == null)
	    throw new IllegalArgumentException();
	LogicBytecodeFile bytecode = this.loaderImplObj.getBytecodeFor(linker);
	if (bytecode != null) {
	    logger.debug("loading linker & bytecode for definition: %s, %s", linker, bytecode);
	    Iterator<ImportsEntry> itx = linker.importsTable.iterator();
	    while (itx.hasNext()) {
		ImportsEntry importsDecl = itx.next();
		boolean flag = false;
		Iterator<LinkerObjectFile> itx1 = loadedObjMap.keySet().iterator();
		while (itx1.hasNext()) {
		    LinkerObjectFile loadedLinker = itx1.next();
		    if (loadedLinker.exports(importsDecl.name)) {
			this.dependencyMap.add(linker, loadedLinker);
			flag = true;
			break;
		    }
		}
		if (!flag) {
		    logger.debug("looking for missing symbol declaration: `%s`", importsDecl.name);
		    LinkerObjectFile loading = null;
		    try {
			if (load(callback, loading = loaderImplObj.getBytecodeFor(importsDecl.name)) == null)
			    throw new UnsatisfiedImportException(this, String.format("Could not load bytecode for import `%s`",
				    importsDecl.name));
		    } catch (Exception e) {
			if (e instanceof UnsatisfiedImportException)
			    throw (UnsatisfiedImportException) e;
			throw new UnsatisfiedImportException(this, String.
				format("Could not load bytecode for import `%s`", importsDecl.name), e);
		    }
		    if (loading != null) {
			this.dependencyMap.add(linker, loading);
			logger.debug("loaded symbol declaration for `%s`: linker %s", importsDecl.name, loading);
		    } else
			throw new UnsatisfiedImportException(this, String.format("Did not load bytecode for import `%s`", importsDecl.name));
		}
	    }
	    logger.debug("allocating closure for loaded: %s, %s", linker, bytecode);
	    VMClosure allocClosure = null;
	    try {
		allocClosure = closureFactory.create(this, linker, bytecode);
	    } catch (Exception e) {
		throw new VMException("Exception when generating closure", e);
	    }
	    logger.debug("initializing closure");
	    allocClosure.load(callback);
	    logger.debug("done initializing closure");
	    closureMap.put(linker, allocClosure);
	    loadedObjMap.put(linker, bytecode);
	    return allocClosure;
	} else
	    throw new UnsatisfiedImportException(this, String.format("Could not find bytecode for linker `%s`", linker.toString()));
    }

    @Override
    public VMClosure getClosure(LinkerObjectFile linker) {
	return closureMap.get(linker);
    }

    @Override
    public void unload(LinkerObjectFile linker) throws UnsatisfiedImportException {
	if (linker == null)
	    throw new IllegalArgumentException();
	logger.debug("unloading linker %s", linker);
	if (closureMap.containsKey(linker)) {
	    logger.debug("checking dependency map for linker %s", linker);
	    Iterator<Entry<LinkerObjectFile, LinkerObjectFile>> itx = this.dependencyMap.iterator();
	    while (itx.hasNext()) {
		Entry<LinkerObjectFile, LinkerObjectFile> entry = itx.next();
		if (entry.getValue().equals(linker))
		    throw new UnsatisfiedImportException(this, String.format("cannot unload `%s`: the source `%s` is dependant", linker.
			    source(), entry.getKey().source()));
	    }

	    logger.debug("unloading %s", linker);
	    closureMap.remove(linker);
	    loadedObjMap.remove(linker);
	    dependencyMap.remove(linker);
	}
    }

    @Override
    public VMType defineJType(VMClosure closure, String handleTypeName, Class<?> klazz) throws VMException {
	logger.debug("defining jtype: name `%s`, class %s", handleTypeName, klazz.getName());
	VMType defn = this.globalTypesIndex.get(closure, handleTypeName);
	if (defn == null)
	    try {
		defn = defineType(closure, handleTypeName);
	    } catch (VMException e) {
		throw new VMException(this, "exception when defining required type reference for native type", e);
	    }
	BaseVMUserType asMutable = (BaseVMUserType) defn;
	asMutable.setKlazzType(this, klazz);
	return asMutable;
    }

    @Override
    public VMType defineJType(VMClosure closure, String handleTypeName, VMType superHandleType, Class<?> klazz) throws VMException {
	logger.debug("defining jtype: name `%s`, class %s", handleTypeName, klazz.getName());
	VMType defn = this.globalTypesIndex.get(closure, handleTypeName);
	if (defn == null)
	    try {
		defn = defineType(closure, handleTypeName, superHandleType);
	    } catch (VMException e) {
		throw new VMException(this, "exception when defining required type reference for native type", e);
	    }
	else if (defn.supertype() != superHandleType)
	    throw new VMException(this, String.format("expected super-type `%s`, but got super-type `%s` from code declaration",
		    superHandleType.name(), defn.supertype().name()));

	BaseVMUserType asMutable = (BaseVMUserType) defn;
	asMutable.setKlazzType(this, klazz);
	return asMutable;
    }

    @Override
    public boolean defineJFunc(String funcName, VMJFunction impl) {
	if (this.jfuncTable.containsKey(funcName))
	    return false;
	logger.debug("defining jfunc: name `%s`, impl %s", funcName, impl);
	this.jfuncTable.put(funcName, impl);
	return true;
    }

    @Override
    public VMType getJType(final VMClosure definer, Class<?> klazz) {
	if (klazz == null)
	    return BaseVMPrimitiveType.TYPE_NOTHING;
	if (klazz == BaseVMFunctionPointerImpl.class)
	    return BaseVMPrimitiveType.TYPE_CODE;
	if (klazz == Boolean.class || klazz == boolean.class)
	    return BaseVMPrimitiveType.TYPE_BOOLEAN;
	if (klazz == Integer.class || klazz == int.class)
	    return BaseVMPrimitiveType.TYPE_INTEGER;
	if (klazz == Double.class || klazz == double.class)
	    return BaseVMPrimitiveType.TYPE_REAL;
	if (klazz == String.class)
	    return BaseVMPrimitiveType.TYPE_STRING;

	final LinkedList< VMType> leaves = this.globalTypesIndex.leaves(definer);
	if (leaves == null || leaves.isEmpty())
	    return BaseVMPrimitiveType.TYPE_HANDLE;

	final LinkedList< VMType> v0 = new LinkedList<>(), v1 = new LinkedList<>();
	boolean flag = false;

	Iterator<VMType> itx = leaves.iterator();
	while (itx.hasNext()) {
	    VMType t = itx.next();
	    if (t.jklazz() == null) {
		if (t.supertype() != null)
		    v0.add(t.supertype());
	    } else if (t.jklazz().isAssignableFrom(klazz))
		return t;
	    else if (t.supertype() != null)
		v0.add(t.supertype());
	}

	while (!v0.isEmpty() || !v1.isEmpty()) {
	    flag = v0.isEmpty();
	    itx = (flag) ? v1.iterator() : v0.iterator();
	    while (itx.hasNext()) {
		VMType t = itx.next();

		if (t.jklazz() == null) {
		    if (t.supertype() != null)
			if (flag)
			    v0.add(t.supertype());
			else
			    v1.add(t.supertype());
		} else if (t.jklazz().isAssignableFrom(klazz))
		    return t;
		else if (t.supertype() != null)
		    if (flag)
			v0.add(t.supertype());
		    else
			v1.add(t.supertype());
	    }
	}
	return BaseVMPrimitiveType.TYPE_HANDLE;
    }

    @Override
    public VMType getType(VMClosure definer, String typeName) {
	return this.globalTypesIndex.get(definer, typeName);
    }

    @Override
    public void loaded(final List<LinkerObjectFile> ptr) {
	if (ptr == null)
	    throw new IllegalArgumentException("destination List<> pointer required");
	synchronized ( this.loadedObjMap ) {
	    ptr.addAll(this.loadedObjMap.keySet());
	}
    }

    public HashMap<LinkerObjectFile, LogicBytecodeFile> loaded() {
	return this.loadedObjMap;
    }

    protected Log logger() {
	return (this.logger != null) ? this.logger : Log.VoidLog.INSTANCE;
    }

    @Override
    public VMType defineType(final VMClosure definer, String nameOfType) throws VMException {
	VMType defn = this.globalTypesIndex.get(definer, nameOfType);
	if (defn != null)
	    throw new DuplicateTypeNameException(nameOfType, "the type already exists in the closure, cannot redeclare");
	try {
	    defn = this.typeFactory.create(this, definer.getLinker(), definer.getBytecode(), definer, nameOfType);
	} catch (Exception e) {
	    throw new VMException(this, "exception when constructing type object", e);
	}
	this.globalTypesIndex.put(definer, defn);
	return defn;
    }

    @Override
    public VMType defineType(VMClosure definer, String nameOfType, VMType superType) throws VMException {
	VMType defn = this.globalTypesIndex.get(definer, nameOfType);
	if (defn != null)
	    throw new DuplicateTypeNameException(nameOfType, "the type already exists in the closure, cannot redeclare");
	try {
	    defn = this.typeFactory.create(this, definer.getLinker(), definer.getBytecode(), definer, nameOfType, superType);
	} catch (Exception e) {
	    throw new VMException(this, "exception when constructing type object", e);
	}
	this.globalTypesIndex.put(definer, defn);
	return defn;
    }

    protected Pair< LinkerObjectFile, VMClosure> getlib(String name) {
	Iterator<Entry<LinkerObjectFile, VMClosure>> itx = closureMap.entrySet().iterator();
	while (itx.hasNext()) {
	    Entry<LinkerObjectFile, VMClosure> obj = itx.next();
	    if (obj.getKey().exports(name))
		return new Pair<>(obj.getKey(), obj.getValue());
	}
	return null;
    }

    protected VMType gettype(final VMClosure closure, Class<?> klazz) {
	if (klazz == null || klazz == Void.class)
	    return BaseVMPrimitiveType.TYPE_NOTHING;
	if (VMFunctionPointer.class.isAssignableFrom(klazz) || VMFunction.class.isAssignableFrom(klazz))
	    return BaseVMPrimitiveType.TYPE_CODE;
	if (Boolean.class.isAssignableFrom(klazz) || klazz.equals(boolean.class))
	    return BaseVMPrimitiveType.TYPE_BOOLEAN;
	if (Integer.class.isAssignableFrom(klazz) || klazz.equals(int.class))
	    return BaseVMPrimitiveType.TYPE_INTEGER;
	if (Double.class.isAssignableFrom(klazz) || klazz.equals(double.class))
	    return BaseVMPrimitiveType.TYPE_REAL;
	if (String.class.isAssignableFrom(klazz))
	    return BaseVMPrimitiveType.TYPE_STRING;

	LinkedList<VMType> types = closure.types().getTypes();
	Iterator<VMType> itx = types.iterator();
	while (itx.hasNext()) {
	    VMType type = itx.next();
	    if (type.jklazz() != null) { // if the type has a specific klazz...
		if (type.jklazz().isAssignableFrom(klazz))
		    return type;
	    } else if (type.supertype() != null) { // no klazz, but has supertype...
		VMType supertype = type.supertype();
		while (supertype != null) {
		    if (supertype.jklazz() != null) { // ... and the supertype defines a klazz ...
			if (supertype.jklazz().isAssignableFrom(klazz))
			    return type;
			break; // ... the supertype is a different type
		    }
		    supertype = supertype.supertype();
		}
	    }
	}

	return BaseVMPrimitiveType.TYPE_HANDLE;
    }

    protected VMType gettype(final VMClosure closure, Object object) {
	return gettype(closure, (object != null) ? object.getClass() : null);
    }

    protected boolean checkcast(VMType dstType, VMType srcType) throws VMException {
	if (dstType == srcType)
	    return true;

	if (dstType == BaseVMPrimitiveType.TYPE_ANY)
	    return srcType != BaseVMPrimitiveType.TYPE_NOTHING;

	if (dstType == BaseVMPrimitiveType.TYPE_HANDLE)
	    return srcType == BaseVMPrimitiveType.TYPE_HANDLE || srcType instanceof BaseVMUserType;

	VMType uptype = srcType;
	while (uptype != null && ((uptype = uptype.supertype()) != null))
	    if (uptype == dstType)
		return true;
	return false;
    }

    @Override
    public boolean canCastToType(VMType destType, VMType srcType) throws VMException {
	if (destType == null || srcType == null)
	    return false;
	return checkcast(destType, srcType);
    }

    @Override
    public VMFunctionPointer getFunction(LinkerObjectFile linker, String nameOfFunction) throws LinkerNotLoadedException, NoSuchFunctionException {
	VMClosure closureObj = closureMap.get(linker);
	if (closureObj == null)
	    throw new LinkerNotLoadedException(this, linker);
	return getfunc(closureObj, nameOfFunction);
    }

    @Override
    public VMInvokable invoke(VMDebugCallback debug, VMFunctionPointer fn) {
	return invoke(debug, fn.dereference().closure(), fn);
    }

    @Override
    public VMInvokable invoke(VMDebugCallback debug, VMFunction function) {
	return invoke(debug, function.closure(), new BaseVMFunctionPointerImpl(function));
    }

    @Override
    public Object result(VMInvokable inst) {
	return ((BaseVMInvokableImpl) inst).result();
    }

    @Override
    public Object[] resultArray(VMInvokable inst) {
	Object resultObj = ((BaseVMInvokableImpl) inst).result();
	if (!(resultObj instanceof BaseVMArrayImpl))
	    return new Object[] {resultObj};
	BaseVMArrayImpl asArrayObj = (BaseVMArrayImpl) resultObj;
	Object[] unboxedResult = new Object[ asArrayObj.sizeOfArray ];
	for (int i = 0, j = unboxedResult.length; i < j; i++)
	    unboxedResult[i] = asArrayObj.arrayValues[i];
	return unboxedResult;
    }

    @Override
    public void reset(VMInvokable inst) {
	((BaseVMInvokableImpl) inst).reset();
    }

    @Override
    public boolean yielded(VMInvokable inst) {
	return ((BaseVMInvokableImpl) inst).yielded();
    }

    @Override
    public Object yield(VMInvokable inst) {
	return ((BaseVMInvokableImpl) inst).yieldValue();
    }

    protected BaseVMFunctionPointerImpl getfunc(VMClosure closure, String nameOfFunction) throws NoSuchFunctionException {
	VMFunction funcDirect = closure.getFunction(nameOfFunction);

	if (funcDirect == null) {
	    Iterator<ImportsEntry> itx = closure.getLinker().importsTable.iterator();
	    while (itx.hasNext()) {
		ImportsEntry e = itx.next();
		if ((e.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_FUNCTION) == 0)
		    continue;

		if ((e.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD) == 0 && !e.labels.contains(nameOfFunction))
		    continue;

		Pair<LinkerObjectFile, VMClosure> obj = getlib(e.name);
		if (obj == null)
		    continue;

		Iterator<ExportsEntry> itx1 = obj.a().exportsTable.iterator();
		while (itx1.hasNext()) {
		    ExportsEntry e1 = itx1.next();
		    if ((e1.typeflags & LinkerObjectFile.TYPEFLAGS_EXPORTS_FUNCTION) != 0 && e1.labels.contains(nameOfFunction)) {
			funcDirect = obj.b().getFunction(nameOfFunction);
			break;
		    }
		}
	    }
	}

	if (funcDirect == null)
	    throw new NoSuchFunctionException(this, null, String.format("No such function: %s", nameOfFunction));

	BaseVMFunctionPointerImpl allocPtrObj = new BaseVMFunctionPointerImpl(funcDirect);
	return allocPtrObj;
    }

    protected GlobalBox getglobal(VMClosure closure, String nameOfGlobal) throws UnsatisfiedImportException {
	GlobalBox global = closure.globals().getGlobal(nameOfGlobal);
	if (global == null) {
	    Iterator<ImportsEntry> itx = closure.getLinker().importsTable.iterator();
	    while (itx.hasNext()) {
		ImportsEntry e = itx.next();
		if ((e.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL) == 0)
		    continue;

		if ((e.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD) == 0 && !e.labels.contains(nameOfGlobal))
		    continue;

		Pair<LinkerObjectFile, VMClosure> obj = getlib(e.name);
		if (obj == null)
		    continue;

		Iterator<ExportsEntry> itx1 = obj.a().exportsTable.iterator();
		boolean flag = false;
		while (itx1.hasNext()) {
		    ExportsEntry e1 = itx1.next();
		    if ((e1.typeflags & LinkerObjectFile.TYPEFLAGS_EXPORTS_GLOBAL) == 0 || !e1.labels.contains(nameOfGlobal))
			continue;
		    flag = true;
		    break;
		}

		if (flag) {
		    global = obj.b().globals().getGlobal(nameOfGlobal);
		    break;
		}
	    }
	}

	if (global == null)
	    throw new UnsatisfiedImportException(this, String.format("No such global: %s", nameOfGlobal));
	return global;
    }

    protected BaseVMInvokableImpl invoke(VMDebugCallback debug, VMClosure closure, VMFunctionPointer fn) {
	BaseVMInvokableImpl obj = new BaseVMInvokableImpl(this, globalLock, fn);
	obj.debugger(debug);
	return obj;
    }

}
