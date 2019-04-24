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
import logicbyte.vm.exception.LinkerNotLoadedException;
import logicbyte.vm.exception.NoSuchFunctionException;
import logicbyte.vm.exception.UnsatisfiedImportException;
import logicbyte.vm.exception.VMException;
import logicbyte.vm.factory.VMClosureFactory;
import logictype.BiDiMap;
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
     * The loader reference which shall be used to find and load linkers / bytecode data
     */
    final DynamicLibraryFile loaderImplObj;

    final VMClosureFactory closureFactory;

    /**
     * The bi-directional map of all loaded linker &amp; bytecode objects
     */
    final BiDiMap< LinkerObjectFile, LogicBytecodeFile> loadedObjMap;
    /**
     * The map of all linkers to closures
     */
    final Map<LinkerObjectFile, VMClosure> closureMap;

    /**
     * The VM global map of all type names, mapped to Java class types
     */
    final BiDiMap<String, Class<?>> jtypeTable;
    /**
     * The VM global map of all type names mapped to inherited types
     */
    final Map<String, List<String>> jtypeSuperTypeTable;

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
     */
    public BaseLogicVMInstance(DynamicLibraryFile loaderObj, VMClosureFactory closureFactory) {
	this.loaderImplObj = loaderObj;
	this.closureFactory = closureFactory;
	this.loadedObjMap = new BiDiMap<>();
	this.closureMap = new HashMap<>();
	this.jtypeTable = new BiDiMap<>();
	this.jtypeSuperTypeTable = new HashMap<>();
	this.jfuncTable = new BiDiMap<>();
    }

    @Override
    public VMClosure load(final VMDebugCallback callback, final LinkerObjectFile linker) throws InterruptedException, DynamicLibraryException, VMException, UnsatisfiedImportException {
	if (linker == null)
	    throw new IllegalArgumentException();
	LogicBytecodeFile bytecode = this.loaderImplObj.getBytecodeFor(linker);
	if (bytecode != null) {
	    Iterator<ImportsEntry> itx = linker.importsTable.iterator();
	    while (itx.hasNext()) {
		ImportsEntry importsDecl = itx.next();
		boolean flag = false;
		Iterator<LinkerObjectFile> itx1 = loadedObjMap.keySet().iterator();
		while (itx1.hasNext()) {
		    LinkerObjectFile loadedLinker = itx1.next();
		    if (loadedLinker.exports(importsDecl.name)) {
			flag = true;
			break;
		    }
		}
		if (!flag)
		    try {
			if (load(callback, loaderImplObj.getBytecodeFor(importsDecl.name)) == null)
			    throw new UnsatisfiedImportException(this, String.format("Could not load bytecode for import `%s`",
				    importsDecl.name));
		    } catch (Exception e) {
			if (e instanceof UnsatisfiedImportException)
			    throw (UnsatisfiedImportException) e;
			throw new UnsatisfiedImportException(this, String.
				format("Could not load bytecode for import `%s`", importsDecl.name), e);
		    }
	    }
	    VMClosure allocClosure = null;
	    try {
		allocClosure = closureFactory.create(this, linker, bytecode);
	    } catch (Exception e) {
		throw new VMException("Exception when generating closure", e);
	    }
	    allocClosure.load(callback);
	    closureMap.put(linker, allocClosure);
	    loadedObjMap.put(linker, bytecode);
	    return allocClosure;
	} else
	    throw new UnsatisfiedImportException(this, String.format("Could not find bytecode for linker `%s`", linker.toString()));
    }

    @Override
    public boolean defineJType(String handleTypeName, Class<?> klazz) {
	if (this.jtypeTable.containsKey(handleTypeName))
	    return false;
	this.jtypeTable.put(handleTypeName, klazz);
	return true;
    }

    @Override
    public boolean defineJType(String handleTypeName, String superTypeName, Class<?> klazz) throws IllegalArgumentException {
	if (this.jtypeTable.containsKey(handleTypeName))
	    return false;
	if (!this.jtypeTable.containsKey(superTypeName))
	    throw new IllegalArgumentException(String.format("No such supertype %s", superTypeName));

	Class<?> knownSuperTypeKlazz = this.jtypeTable.get(superTypeName);

	if (!knownSuperTypeKlazz.isAssignableFrom(klazz))
	    throw new IllegalArgumentException(String.format("Supertype %s (class %s) is not assignable to class %s", superTypeName,
		    knownSuperTypeKlazz.getName(), klazz.getName()));
	this.jtypeTable.put(handleTypeName, klazz);

	if (!this.jtypeSuperTypeTable.containsKey(superTypeName))
	    this.jtypeSuperTypeTable.put(superTypeName, new LinkedList<>());
	this.jtypeSuperTypeTable.get(superTypeName).add(handleTypeName);
	return true;
    }

    @Override
    public boolean defineJFunc(String funcName, VMJFunction impl) {
	if (this.jfuncTable.containsKey(funcName))
	    return false;
	this.jfuncTable.put(funcName, impl);
	return true;
    }

    @Override
    public String getJTypeName(Class<?> klazz) {
	if (klazz == null)
	    return "nothing";
	if (klazz == BaseVMFunctionPointerImpl.class)
	    return "code";
	if (klazz == Boolean.class || klazz == boolean.class)
	    return "boolean";
	if (klazz == Integer.class || klazz == int.class)
	    return "integer";
	if (klazz == Double.class || klazz == double.class)
	    return "real";
	if (klazz == String.class)
	    return "string";

	String typename = this.jtypeTable.reverse().get(klazz);
	return (typename != null) ? typename : "handle";
    }

    @Override
    public boolean isJType(Class<?> klazz, String typename) {
	if (getJTypeName(klazz).equals(typename))
	    return true;
	if (klazz.getSuperclass() != null)
	    return isJType(klazz.getSuperclass(), typename);
	return false;
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

    protected int gettypenum(VMClosure closure, Object object) {
	if (object == null)
	    return BaseVMTypesTable.TYPENUMBER_NOTHING; // NOTHING
	if (object instanceof VMFunctionPointer || object instanceof VMFunction)
	    return BaseVMTypesTable.TYPENUMBER_CODE; // CODE
	if (object instanceof Boolean || object.getClass().equals(boolean.class))
	    return BaseVMTypesTable.TYPENUMBER_BOOLEAN; // BOOLEAN
	if (object instanceof Integer || object.getClass().equals(int.class))
	    return BaseVMTypesTable.TYPENUMBER_INTEGER; // INTEGER
	if (object instanceof Double || object.getClass().equals(double.class))
	    return BaseVMTypesTable.TYPENUMBER_REAL; // REAL
	if (object instanceof String || object.getClass().equals(String.class))
	    return BaseVMTypesTable.TYPENUMBER_STRING; // STRING

	String jtypename = this.jtypeTable.reverse().get(object.getClass());
	if (jtypename != null) {
	    int jtypenum = closure.types().getNumberOf(jtypename);
	    return (jtypenum != -1) ? jtypenum : BaseVMTypesTable.TYPENUMBER_HANDLE;
	}
	return BaseVMTypesTable.TYPENUMBER_HANDLE; // HANDLE (generic type)
    }

    protected boolean checkcast(int destType, int srcType) {
	return (destType == BaseVMTypesTable.TYPENUMBER_HANDLE)
		? (srcType == BaseVMTypesTable.TYPENUMBER_HANDLE || srcType >= 7)
		: (destType == srcType);
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
	    throw new NoSuchFunctionException(this, String.format("No such function: %s", nameOfFunction));

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
