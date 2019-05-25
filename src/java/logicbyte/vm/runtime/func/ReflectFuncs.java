package logicbyte.vm.runtime.func;

import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMFunctionPointer;
import logicbyte.vm.VMGlobalsTable.GlobalBox;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMJFunctionLambda;
import logicbyte.vm.exception.ThrowObjectException;
import logicbyte.vm.exception.VMException;
import logicbyte.vm.runtime.BaseVMArrayImpl;
import logicbyte.vm.runtime.BaseVMJFunctionImpl;
import logicbyte.vm.runtime.func.CoroutineFuncs.CoroutineImpl;
import logicscript.assembler.writer.ChunkAssembler;
import logicscript.ast.Chunk;
import logicscript.parser.LogicScriptParser;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public class ReflectFuncs {

    /**
     * <p>
     * Interface method for the <code>bytecode</code> VM type, which is either one of:</p>
     *
     * <ul>
     * <li>A <code>ReflectASMImpl</code> object, which is a weak-reference to the VM object(s) which were loaded by direct or indirect call
     * to the <code>load</code> method; or</li>
     * <li>A <code>ReflectASMDynamicImpl</code> object, which is a strong-reference to the dynamically generated source (from
     * native-function <code>Reflect_Compile</code> whose body is <code>unload</code>-ed when the reference is collected.</li>
     * </ul>
     *
     */
    public static interface ReflectASMInterface {

	LinkerObjectFile linker();

	LogicBytecodeFile bytecode();
    }

    public static class ReflectASMDynamicImpl implements ReflectASMInterface {

	final WeakReference<LogicVM> vmRef;
	final LinkerObjectFile linker;
	final LogicBytecodeFile bytecode;

	public ReflectASMDynamicImpl(final LogicVM hostVm, final LinkerObjectFile linker, final LogicBytecodeFile bytecode) {
	    this.vmRef = new WeakReference<>(hostVm);
	    this.linker = linker;
	    this.bytecode = bytecode;
	}

	@Override
	public LinkerObjectFile linker() {
	    return this.linker;
	}

	@Override
	public LogicBytecodeFile bytecode() {
	    return this.bytecode;
	}

	@Override
	protected void finalize() throws Throwable {
	    if (this.vmRef != null && !this.vmRef.isEnqueued() && this.vmRef.get() != null) {
		LogicVM vmInst = this.vmRef.get();
		// FIXME: while finalize()V; allows for throwing anything, we shouldn't throw that dynamically
		// generated code couldn't be unloaded; this is likely to cause issues during object deconstruct.
		vmInst.unload(this.linker);
	    }
	    super.finalize();
	}

    }

    public static class ReflectASMImpl implements ReflectASMInterface {

	final WeakReference<LinkerObjectFile> linkerRef;
	final WeakReference<LogicBytecodeFile> bytecodeRef;

	public ReflectASMImpl(final LinkerObjectFile linker, final LogicBytecodeFile bytecode) {
	    this.linkerRef = new WeakReference<>(linker);
	    this.bytecodeRef = new WeakReference<>(bytecode);
	}

	@Override
	public LinkerObjectFile linker() {
	    return (this.linkerRef != null && !this.linkerRef.isEnqueued()) ? this.linkerRef.get() : null;
	}

	@Override
	public LogicBytecodeFile bytecode() {
	    return (this.bytecodeRef != null && !this.bytecodeRef.isEnqueued()) ? this.bytecodeRef.get() : null;
	}

    }

    public static class ReflectInvokeImpl {

	VMFunctionPointer funcPtr;

	Object[] callArgs;

    }

    public static class ReflectInvokeApplyFuncInternal implements VMJFunctionLambda {

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] arguments) throws ThrowObjectException, Exception {
	    if (arguments.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(arguments[0] instanceof ReflectInvokeImpl))
		throw new ThrowObjectException("argument 1 not invoke");
	    ReflectInvokeImpl invokable = (ReflectInvokeImpl) arguments[0];

	    final VMType[] argTypes = invokable.funcPtr.argumentTypesOfFunction();

	    if (argTypes.length != 0 && invokable.callArgs == null)
		throw new ThrowObjectException("call arguments required");

	    for (int i = 0, j = argTypes.length; i < j; i++) {
		VMType requiredType = argTypes[i];
		VMType argumentType = vm.getJType(caller.closure(),
			(invokable.callArgs[i] != null) ? invokable.callArgs[i].getClass() : null);
		if (!vm.canCastToType(requiredType, argumentType))
		    throw new ThrowObjectException(String.format("argument at index %s not of type %s, got %s", i,
			    requiredType, argumentType));
	    }

	    final CoroutineImpl allocCoObj = new CoroutineImpl();
	    allocCoObj.funcPtr = invokable.funcPtr;
	    allocCoObj.callableObj = vm.invoke(debug, invokable.funcPtr);
	    allocCoObj.callableObj.invoke(debug, invokable.callArgs);
	    return allocCoObj;
	}
    }

    public static class ReflectInvokeMutateFuncInternal<T> implements VMJFunctionLambda {

	final Class<? extends T> theTypeOfT;

	public ReflectInvokeMutateFuncInternal(Class<? extends T> theTypeOfT) {
	    this.theTypeOfT = theTypeOfT;
	}

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] arguments) throws ThrowObjectException, Exception {
	    if (arguments.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(arguments[0] instanceof ReflectInvokeImpl))
		throw new ThrowObjectException("argument 1 not invoke");
	    if (!(arguments[1] instanceof Integer))
		throw new ThrowObjectException("argument 2 not integer");
	    if (!theTypeOfT.isAssignableFrom(arguments[2].getClass()))
		throw new ThrowObjectException(String.format("argument 3 not %s", vm.getJType(caller.closure(), theTypeOfT).name()));

	    ReflectInvokeImpl invokable = (ReflectInvokeImpl) arguments[0];
	    if (invokable.callArgs == null)
		invokable.callArgs = new Object[ invokable.funcPtr.dereference().bytecode().numargs() ];

	    final int slotOfArg = (int) arguments[1];
	    if (slotOfArg < 0 || slotOfArg >= invokable.callArgs.length)
		throw new ThrowObjectException("argument index out of bounds");
	    invokable.callArgs[slotOfArg] = arguments[2];

	    return null;
	}
    }

    public static class ReflectGlobalReadFuncInternal<R> implements VMJFunctionLambda {

	final Class<? extends R> theTypeOfR;

	public ReflectGlobalReadFuncInternal(Class<? extends R> theTypeOfR) {
	    this.theTypeOfR = theTypeOfR;
	}

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] arguments) throws ThrowObjectException, Exception {
	    if (arguments.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(arguments[0] instanceof ReflectASMInterface))
		throw new ThrowObjectException("argument 1 not bytecode");
	    if (!(arguments[1] instanceof String))
		throw new ThrowObjectException("argument 2 not string");

	    final ReflectASMInterface asmObj = (ReflectASMInterface) arguments[0];
	    final LinkerObjectFile linker = asmObj.linker();
	    if (linker == null)
		throw new ThrowObjectException("bytecode unloaded");

	    final String nameOfGlobal = (String) arguments[1];
	    final VMClosure closureObj = vm.getClosure(linker);
	    if (closureObj == null)
		throw new ThrowObjectException("bytecode unloaded");
	    final GlobalBox valueBox = closureObj.globals().getGlobal(nameOfGlobal);
	    if (valueBox == null)
		throw new ThrowObjectException("global unloaded");
	    if (valueBox.globalIsArrayType)
		throw new ThrowObjectException("global is array type");

	    final Object valueOfGlobal = valueBox.valueInGlobalBox;
	    if (!theTypeOfR.isAssignableFrom(valueOfGlobal.getClass()))
		throw new ThrowObjectException(String.format("global value %s not of type %s", vm.getJType(valueBox.typeOfGlobalValue.
			closure(), valueOfGlobal.getClass()), vm.getJType(caller.closure(), theTypeOfR)));
	    return (R) valueOfGlobal;
	}

    }

    public static class ReflectGlobalArrayReadFuncInternal<R> implements VMJFunctionLambda {

	final Class<? extends R> theTypeOfR;

	public ReflectGlobalArrayReadFuncInternal(Class<? extends R> theTypeOfR) {
	    this.theTypeOfR = theTypeOfR;
	}

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] arguments) throws ThrowObjectException, Exception {
	    if (arguments.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(arguments[0] instanceof ReflectASMInterface))
		throw new ThrowObjectException("argument 1 not bytecode");
	    if (!(arguments[1] instanceof String))
		throw new ThrowObjectException("argument 2 not string");
	    if (!(arguments[2] instanceof Integer))
		throw new ThrowObjectException("argument 3 not integer");

	    final ReflectASMInterface asmObj = (ReflectASMInterface) arguments[0];
	    final LinkerObjectFile linker = asmObj.linker();
	    if (linker == null)
		throw new ThrowObjectException("bytecode unloaded");

	    final String nameOfGlobal = (String) arguments[1];
	    final VMClosure closureObj = vm.getClosure(linker);
	    if (closureObj == null)
		throw new ThrowObjectException("bytecode unloaded");
	    final GlobalBox valueBox = closureObj.globals().getGlobal(nameOfGlobal);
	    if (valueBox == null)
		throw new ThrowObjectException("global unloaded");

	    if (!valueBox.globalIsArrayType)
		throw new ThrowObjectException("global not array type");

	    final BaseVMArrayImpl arrayObj = (BaseVMArrayImpl) valueBox.valueInGlobalBox;
	    final int indexOfArray = (int) arguments[2];
	    if (indexOfArray < 0 || indexOfArray >= arrayObj.sizeOfArray)
		throw new ThrowObjectException("array index out of bounds");

	    final Object valueOfArraySlot = arrayObj.arrayValues[indexOfArray];
	    if (!theTypeOfR.isAssignableFrom(valueOfArraySlot.getClass()))
		throw new ThrowObjectException(String.format("global value %s not of type %s", vm.getJType(valueBox.typeOfGlobalValue.
			closure(), valueOfArraySlot.getClass()), vm.getJType(caller.closure(), theTypeOfR)));
	    return (R) valueOfArraySlot;
	}

    }

    public static void apply(final LogicVM vm, final VMClosure targetClosure) throws VMException, NoSuchMethodException {

	vm.defineJType(targetClosure, "bytecode", ReflectASMInterface.class);
	vm.defineJType(targetClosure, "invoke", ReflectInvokeImpl.class);

	vm.defineJFunc("Reflect_Compile", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(args[0] instanceof String))
		throw new ThrowObjectException("argument 1 not string");
	    final String source = (String) args[0];

	    final LogicScriptParser parser = new LogicScriptParser(new StringReader(source));
	    final Chunk allocChunkObj = new Chunk();
	    parser.SCRIPT(allocChunkObj);

	    final ChunkAssembler chunkAssembler = new ChunkAssembler();
	    chunkAssembler.setShouldGenerateDebugSymbols(true);

	    final LinkerObjectFile allocLinkerObj = new LinkerObjectFile("<dynamic>");
	    final LogicBytecodeFile allocBytecodeObj = new LogicBytecodeFile("<dynamic>");

	    chunkAssembler.assembleChunk(allocChunkObj, allocLinkerObj, allocBytecodeObj, null);
	    vmInst.load(debug, allocLinkerObj);

	    return new ReflectASMDynamicImpl(vmInst, allocLinkerObj, allocBytecodeObj);
	}));

	vm.defineJFunc("Reflect_Current", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (exec.access().getCallStackSize() < 1)
		throw new ThrowObjectException("not in function call");
	    VMFunction callerFunc = exec.access().getFunctionOnCallStack(1);
	    if (callerFunc == null)
		throw new ThrowObjectException("cannot locate caller function");
	    VMClosure definerClosure = callerFunc.closure();
	    final ReflectASMInterface alloc = new ReflectASMImpl(definerClosure.getLinker(), definerClosure.getBytecode());
	    return alloc;
	}));

	vm.defineJFunc("Reflect_GetFunc", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(args[0] instanceof ReflectASMInterface))
		throw new ThrowObjectException("argument 1 not bytecode");
	    if (!(args[1] instanceof String))
		throw new ThrowObjectException("argument 2 not string");

	    final ReflectASMInterface asmObj = (ReflectASMInterface) args[0];
	    final LinkerObjectFile linker = asmObj.linker();
	    if (linker == null)
		throw new ThrowObjectException("bytecode unloaded");

	    VMFunctionPointer ptr = vmInst.getFunction(linker, (String) args[1]);
	    if (ptr == null)
		throw new ThrowObjectException("no such function");
	    return ptr;
	}));

	vm.defineJFunc("Reflect_GetFuncNumArgs", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(args[0] instanceof VMFunctionPointer))
		throw new ThrowObjectException("argument 1 not code");
	    return ((VMFunctionPointer) args[0]).dereference().bytecode().numargs();
	}));

	vm.defineJFunc("Reflect_GetFuncArgType", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(args[0] instanceof VMFunctionPointer))
		throw new ThrowObjectException("argument 1 not code");
	    if (!(args[1] instanceof Integer))
		throw new ThrowObjectException("argument 2 not integer");
	    final VMFunctionPointer ptr = (VMFunctionPointer) args[0];
	    final VMType[] typeTable = ptr.argumentTypesOfFunction();
	    final int slot = (int) args[1];
	    if (slot < 0 || slot >= typeTable.length)
		throw new ThrowObjectException("argument index out of bounds");
	    return typeTable[slot];
	}));

	vm.defineJFunc("Reflect_GetFuncRetnType", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(args[0] instanceof VMFunctionPointer))
		throw new ThrowObjectException("argument 1 not code");
	    return ((VMFunctionPointer) args[0]).returnTypeOfFunction();
	}));

	vm.defineJFunc("Reflect_NewFuncInvoke", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");
	    if (!(args[0] instanceof VMFunctionPointer))
		throw new ThrowObjectException("argument 1 not function");
	    final ReflectInvokeImpl alloc = new ReflectInvokeImpl();
	    alloc.funcPtr = (VMFunctionPointer) args[0];
	    return alloc;
	}));

	vm.defineJFunc("Reflect_InvokeSetArgB", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectInvokeMutateFuncInternal<>(
		Boolean.class)));
	vm.defineJFunc("Reflect_InvokeSetArgI", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectInvokeMutateFuncInternal<>(
		Integer.class)));
	vm.defineJFunc("Reflect_InvokeSetArgR", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectInvokeMutateFuncInternal<>(
		Double.class)));
	vm.defineJFunc("Reflect_InvokeSetArgS", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectInvokeMutateFuncInternal<>(
		String.class)));
	vm.defineJFunc("Reflect_InvokeSetArgH", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectInvokeMutateFuncInternal<>(
		Object.class)));
	vm.defineJFunc("Reflect_InvokeApply", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectInvokeApplyFuncInternal()));

	vm.defineJFunc("Reflect_GetGlobalType", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");

	    if (!(args[0] instanceof ReflectASMInterface))
		throw new ThrowObjectException("argument 1 not bytecode");
	    if (!(args[1] instanceof String))
		throw new ThrowObjectException("argument 2 not string");

	    final ReflectASMInterface asmObj = (ReflectASMInterface) args[0];
	    final LinkerObjectFile linker = asmObj.linker();

	    if (linker == null)
		throw new ThrowObjectException("bytecode unloaded");

	    final String nameOfGlobal = (String) args[1];

	    Iterator<GlobalsEntry> itx0 = linker.globalsTable.iterator();
	    while (itx0.hasNext()) {
		GlobalsEntry g = itx0.next();
		if (g.name.equals(nameOfGlobal))
		    return vmInst.getClosure(linker).types().getTypeOf(g.type);
	    }
	    throw new ThrowObjectException("no such global");
	}));

	vm.defineJFunc("Reflect_GetGlobalArray", BaseVMJFunctionImpl.generateFunctionLambda((vmInst, exec, caller, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("wrong number of arguments to function call");

	    if (!(args[0] instanceof ReflectASMInterface))
		throw new ThrowObjectException("argument 1 not bytecode");
	    if (!(args[1] instanceof String))
		throw new ThrowObjectException("argument 2 not string");

	    final ReflectASMInterface asmObj = (ReflectASMInterface) args[0];
	    final LinkerObjectFile linker = asmObj.linker();

	    if (linker == null)
		throw new ThrowObjectException("bytecode unloaded");

	    final String nameOfGlobal = (String) args[1];
	    final VMClosure closureObj = vmInst.getClosure(linker);
	    if (closureObj == null)
		throw new ThrowObjectException("bytecode unloaded");
	    final GlobalBox valueBox = closureObj.globals().getGlobal(nameOfGlobal);
	    if (valueBox == null)
		throw new ThrowObjectException("global unloaded");
	    return (valueBox.globalIsArrayType) ? ((BaseVMArrayImpl) valueBox.valueInGlobalBox).sizeOfArray : 0;
	}));

	vm.defineJFunc("Reflect_GetGlobalValB", BaseVMJFunctionImpl.generateFunctionLambda(
		new ReflectGlobalReadFuncInternal<>(Boolean.class)));
	vm.defineJFunc("Reflect_GetGlobalValI", BaseVMJFunctionImpl.generateFunctionLambda(
		new ReflectGlobalReadFuncInternal<>(Integer.class)));
	vm.defineJFunc("Reflect_GetGlobalValR", BaseVMJFunctionImpl.
		generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(Double.class)));
	vm.defineJFunc("Reflect_GetGlobalValS", BaseVMJFunctionImpl.
		generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(String.class)));
	vm.defineJFunc("Reflect_GetGlobalValH", BaseVMJFunctionImpl.
		generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(Object.class)));

	vm.defineJFunc("Reflect_GetGlobalArrValB", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(
		Boolean.class)));
	vm.defineJFunc("Reflect_GetGlobalArrValI", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(
		Integer.class)));
	vm.defineJFunc("Reflect_GetGlobalArrValR", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(
		Double.class)));
	vm.defineJFunc("Reflect_GetGlobalArrValS", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(
		String.class)));
	vm.defineJFunc("Reflect_GetGlobalArrValH", BaseVMJFunctionImpl.generateFunctionLambda(new ReflectGlobalReadFuncInternal<>(
		Object.class)));

    }

}
