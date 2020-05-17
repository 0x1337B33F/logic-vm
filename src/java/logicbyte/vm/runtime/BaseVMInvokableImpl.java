package logicbyte.vm.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import logicbyte.bytecode.FunctionOpcodeList;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.Opcodes;
import logicbyte.bytecode.OpcodeParamValueTable.BinaryExpressionParamValueTable;
import logicbyte.bytecode.OpcodeParamValueTable.UnaryExpressionParamValueTable;
import logicbyte.vm.VMClosureTypeMapper;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMFunctionPointer;
import logicbyte.vm.VMGlobalsTable.GlobalBox;
import logicbyte.vm.VMGlobalsTable.GlobalType;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMInvokableAccess;
import logicbyte.vm.VMJFunction;
import logicbyte.vm.VMType;
import logicbyte.vm.exception.NoSuchFunctionException;
import logicbyte.vm.exception.ThrowObjectException;
import logicbyte.vm.exception.UncaughtUserException;
import logicbyte.vm.exception.VMException;
import logictype.Pair;

/**
 *
 * @author Lochie
 */
public class BaseVMInvokableImpl implements VMInvokable {

    protected static class Closure {

	int endOfClosureAddress;
	int startOfClosureLocals;
	int numberOfClosureLocals;
	List<VMType> typesOfClosureLocals;
	List<Boolean> arrayTypeOfClosureLocal;
	List<Object> valuesOfClosureLocals;
    }

    protected static enum FunctionCallContextType {
	BYTECODE, NATIVE;
    }

    protected static class FunctionCallContext {

	FunctionCallContextType contextType;

	int programCounterValue;
	boolean comparisonRegisterValue;

	Closure functionClosureObj;
	Stack<Closure> functionClosureStack;
	Stack<Object> functionStackStack;
	Stack<Pair<Integer, Integer>> functionTryCatchStack;

	Object functionCaughtExceptionObj;
	Object functionYieldObj;

	boolean desiresCallResult;
    }

    final BaseLogicVMInstance instanceObj;

    final Object runLock;

    final Stack<Pair<VMFunction, FunctionCallContext>> callStack;

    final VMFunctionPointer entryFunc;

    final BaseVMInvokableAccessImpl debugAccess;

    volatile VMDebugCallback debuggerObj;

    volatile Object invokeResult;

    volatile boolean yielded;

    public BaseVMInvokableImpl(BaseLogicVMInstance inst, Object runLock, VMFunctionPointer entryFunc) {
	this.instanceObj = inst;
	this.runLock = runLock;
	this.entryFunc = entryFunc;
	this.debugAccess = new BaseVMInvokableAccessImpl(this);
	this.callStack = new Stack<>();
    }

    @Override
    public void debugger(VMDebugCallback callback) {
	this.debuggerObj = callback;
    }

    @Override
    public void invoke(VMDebugCallback debugger, Object[] arguments) throws VMException {
	if (!callStack.isEmpty())
	    throw new VMException("already invoking");
	callfunc(debugger, entryFunc, arguments, null);
    }

    @Override
    public VMInvokableAccess access() {
	return debugAccess;
    }

    @Override
    public boolean advance(VMDebugCallback stepDebugger) throws InterruptedException, VMException {
	if (callStack.isEmpty())
	    return true;
	synchronized (runLock) {
	    try {
		yielded = false;
		Pair<VMFunction, FunctionCallContext> env = callStack.peek();
		exec(env.a(), env.b(), (stepDebugger != null) ? stepDebugger : debuggerObj);
	    } catch (VMException e) {
		throw e;
	    }
	}
	return (yielded || callStack.isEmpty());
    }

    @Override
    public Object tailcall(VMDebugCallback callback, VMFunctionPointer tailcallFunc, Object[] arguments, boolean wantsReturn) throws InterruptedException, VMException {
	final Pair<VMFunction, FunctionCallContext> nativeContext = callStack.peek();
	final int stackDepth = callStack.size();

	final FunctionCallContext nativeCallContext = nativeContext.b();
	nativeCallContext.functionCaughtExceptionObj = null;
	nativeCallContext.desiresCallResult = wantsReturn;

	if (wantsReturn)
	    if (nativeCallContext.functionStackStack == null)
		nativeCallContext.functionStackStack = new Stack<>();
	    else
		nativeCallContext.functionStackStack.clear();

	callfunc(callback, tailcallFunc, arguments, null);

	while (!callStack.isEmpty()
		&& callStack.size() > stackDepth
		&& callStack.peek() != nativeContext)
	    if (advance(callback))
		break;

	if (callStack.peek() != nativeContext)
	    throw new VMException("tailcall stack restore failure");

	if (nativeCallContext.functionCaughtExceptionObj != null)
	    throw new UncaughtUserException(instanceObj, this, nativeCallContext.functionCaughtExceptionObj);

	return (!nativeCallContext.functionStackStack.isEmpty()) ? nativeCallContext.functionStackStack.pop() : null;
    }

    protected Object result() {
	return invokeResult;
    }

    protected void reset() {
	callStack.empty();
	invokeResult = null;
    }

    protected boolean yielded() {
	return yielded;
    }

    protected Object yieldValue() {
	return callStack.peek().b().functionYieldObj;
    }

    private void callfunc(VMDebugCallback debugger, VMFunctionPointer ptr, Object[] arguments, Opcode generatingOp) throws VMException {
	callfunc(debugger, ptr.dereference(), arguments, generatingOp);
    }

    private void callfunc(VMDebugCallback debugger, VMFunction ptr, Object[] arguments, Opcode generatingOp) throws VMException {

	final BytecodeFunctionDecl funcBytecode = ptr.bytecode();

	if (!funcBytecode.hasargs() && (arguments != null && arguments.length != 0))
	    throw new VMException("unexpected function arguments");
	if (funcBytecode.hasargs() && (arguments == null || arguments.length == 0))
	    throw new VMException("required function arguments, got null");

	if (funcBytecode.hasargs() && arguments.length != funcBytecode.numargs())
	    throw new VMException(String.format("expected %s args, got %s args to call", funcBytecode.numargs(), arguments.length));

	if (funcBytecode.isufunc()) {
	    Closure c = new Closure();
	    c.startOfClosureLocals = 0;
	    c.numberOfClosureLocals = (funcBytecode.hasargs()) ? funcBytecode.numargs() : 0;
	    c.numberOfClosureLocals += (funcBytecode.haslocals()) ? funcBytecode.numlocals() : 0;

	    c.typesOfClosureLocals = new LinkedList<>();
	    c.valuesOfClosureLocals = new LinkedList<>();
	    c.arrayTypeOfClosureLocal = new LinkedList<>();

	    final VMClosureTypeMapper typeMapper = ptr.closure().types();

	    if (funcBytecode.hasargs())
		for (int i = 0, j = funcBytecode.numargs(); i < j; i++) {
		    Class<?> klazzOfArgVal = arguments[i].getClass();

		    if (klazzOfArgVal.isArray()) {
			if (!funcBytecode.argarray(i))
			    throw new VMException(String.format("argument at index %s is literal, got array", i));
			Object[] anArrayOfValue = (Object[]) arguments[i];
			Class<?> klazzOfArrayArgVal = klazzOfArgVal.getComponentType();

			VMType wantedType = typeMapper.getTypeOf(funcBytecode.argtype(i));
			VMType arrayKlazzType = instanceObj.gettype(ptr.closure(), klazzOfArrayArgVal);

			if (!instanceObj.checkcast(wantedType, arrayKlazzType))
			    throw new VMException(String.format("argument at index %s not of required array type %s, got %s", i, wantedType,
				    arrayKlazzType));

			final BaseVMArrayImpl allocArrayObj = new BaseVMArrayImpl(arrayKlazzType, anArrayOfValue.length);
			for (int k = 0, l = anArrayOfValue.length; k < l; k++)
			    allocArrayObj.arrayValues[k] = anArrayOfValue[k];

			c.typesOfClosureLocals.add(wantedType);
			c.valuesOfClosureLocals.add(allocArrayObj);
			c.arrayTypeOfClosureLocal.add(true);
		    } else if (arguments[i] instanceof BaseVMArrayImpl) {
			if (!funcBytecode.argarray(i))
			    throw new VMException(String.format("argument at index %s is literal, got array", i));

			BaseVMArrayImpl asVMArray = (BaseVMArrayImpl) arguments[i];
			VMType wantedType = typeMapper.getTypeOf(funcBytecode.argtype(i));
			if (!instanceObj.checkcast(wantedType, asVMArray.typeOfArray))
			    throw new VMException(String.format("argument at index %s not of required array type %s, got %s", i, wantedType,
				    asVMArray.typeOfArray));
			c.typesOfClosureLocals.add(wantedType);
			c.valuesOfClosureLocals.add(asVMArray);
			c.arrayTypeOfClosureLocal.add(true);
		    } else {
			if (funcBytecode.argarray(i))
			    throw new VMException(String.format("argument at %s is array, got literal", i));
			VMType wantedType = typeMapper.getTypeOf(funcBytecode.argtype(i));
			VMType currentType = instanceObj.gettype(ptr.closure(), arguments[i]);
			if (!instanceObj.checkcast(wantedType, currentType))
			    throw new VMException(String.format("argument at index %s not of required type %s, got %s", i, wantedType,
				    currentType));
			c.typesOfClosureLocals.add(wantedType);
			c.valuesOfClosureLocals.add(arguments[i]);
			c.arrayTypeOfClosureLocal.add(false);
		    }
		}

	    if (funcBytecode.haslocals())
		for (int i = 0, j = funcBytecode.numlocals(); i < j; i++) {
		    c.typesOfClosureLocals.add(typeMapper.getTypeOf(funcBytecode.localtype(i)));
		    c.valuesOfClosureLocals.add(null);
		    c.arrayTypeOfClosureLocal.add(funcBytecode.localarray(i));
		}

	    Pair< VMFunction, FunctionCallContext> stacks = enterfunc(debugger, ptr, generatingOp);
	    stacks.b().contextType = FunctionCallContextType.BYTECODE;
	    stacks.b().functionClosureObj = c;
	    stacks.b().functionClosureStack = new Stack<>();
	    stacks.b().functionClosureStack.push(c);
	    stacks.b().functionStackStack = new Stack<>();
	    stacks.b().functionTryCatchStack = new Stack<>();

	} else {
	    VMJFunction jfunc = instanceObj.jfuncTable.get(funcBytecode.name());
	    if (jfunc == null)
		throw new NoSuchFunctionException(instanceObj, this, String.format("native function not found: %s", funcBytecode.
			name()));

	    final VMFunction caller = callStack.peek().a();

	    Pair<VMFunction, FunctionCallContext> nativeContext = enterfunc(debugger, ptr, generatingOp);
	    nativeContext.b().contextType = FunctionCallContextType.NATIVE;

	    Object thrown = null;

	    try {
		Object result = jfunc.invoke(instanceObj, this, caller, (debugger != null) ? debugger : debuggerObj, arguments);
		exitfunc(debugger, nativeContext.a(), result);
	    } catch (IllegalAccessException reflect) {
		throw new VMException("Reflection exception", reflect);
	    } catch (InvocationTargetException ite) {
		if (ite.getCause() instanceof ThrowObjectException)
		    thrown = ((ThrowObjectException) ite.getCause()).getThrown();
		else if (ite.getCause() instanceof UncaughtUserException)
		    thrown = ((UncaughtUserException) ite.getCause()).getUncaught();
		else if (ite.getCause() instanceof VMException)
		    throw (VMException) ite.getCause();
		else
		    thrown = ite.getCause();
	    } catch (ThrowObjectException thrownObject) {
		thrown = thrownObject.getThrown();
	    } catch (Exception uncaughtGeneral) {
		thrown = uncaughtGeneral;
	    }

	    if (thrown != null) {
		/* unwind the native function manually: */
		if (callStack.pop() != nativeContext)
		    throw new VMException("native stack unwind failure");
		if (debugger != null)
		    debugger.unwindfunc(this, callStack.peek().a(), ptr, thrown);
		__raise(debugger, callStack.peek().b(), thrown);
	    }

	}
    }

    protected Pair< VMFunction, FunctionCallContext> enterfunc(VMDebugCallback debugger, VMFunction ptr, Opcode generatingOp) throws VMException {
	if (callStack.size() >= 256)
	    throw new VMException("function call stack overflow");
	if (debugger != null)
	    debugger.enterfunc(this, (!callStack.isEmpty()) ? callStack.peek().a() : null, ptr, generatingOp);

	final Pair< VMFunction, FunctionCallContext> obj = new Pair<>(ptr, new FunctionCallContext());
	callStack.push(obj);
	return obj;
    }

    protected Pair< VMFunction, FunctionCallContext> exitfunc(VMDebugCallback debugger, VMFunction ptr, Object result) throws VMException {
	Pair< VMFunction, FunctionCallContext> obj = callStack.pop();
	if (!obj.a().equals(ptr))
	    throw new VMException("function call stack mismatch");

	if (debugger != null)
	    debugger.exitfunc(this, (!callStack.isEmpty()) ? callStack.peek().a() : null, obj.a(), result);

	if (!callStack.isEmpty()) {
	    if (callStack.peek().b().desiresCallResult) {
		if (result == null)
		    throw new VMException("function call expected result");
		callStack.peek().b().functionStackStack.push(result);
	    }
	} else
	    invokeResult = result;
	return obj;
    }

    protected void exec(VMFunction func, FunctionCallContext boundary, VMDebugCallback debugger) throws VMException {
	FunctionOpcodeList opcodes = func.bytecode().opcodes();
	if (boundary.programCounterValue >= opcodes.size())
	    exitfunc(debugger, func, null);
	else {
	    Opcode op = opcodes.get(boundary.programCounterValue);

	    if (debugger != null)
		debugger.beforeop(this, func, op);

	    switch (op.opcode()) {
		case NOP:
		    break;

		case JMP:
		    exec__JMP(func, boundary, op, debugger);
		    break;
		case DUP:
		    boundary.functionStackStack.push(__peekstackval(boundary, Object.class));
		    break;

		case PUSHB:
		    boundary.functionStackStack.push(__getoparg(op, 0, Boolean.class));
		    break;
		case PUSHI:
		    boundary.functionStackStack.push(__getoparg(op, 0, Integer.class));
		    break;
		case PUSHR:
		    boundary.functionStackStack.push(__getoparg(op, 0, Double.class));
		    break;
		case PUSHL:
		    boundary.functionStackStack.push(__getoparg(op, 0, Long.class));
		    break;
		case PUSHS:
		    boundary.functionStackStack.push(__getoparg(op, 0, String.class));
		    break;

		case POP:
		    __getstackval(boundary, Object.class);
		    break;

		case FUNCPTR:
		    exec__FUNCPTR(func, boundary, op, debugger);
		    break;

		case RETN:
		    exec__RETN(func, boundary, op, debugger);
		    break;
		case YIELD:
		    exec__YIELD(func, boundary, op, debugger);
		    break;

		case CALL:
		    exec__CALL(func, boundary, op, debugger);
		    break;
		case CALLLOCAL:
		    exec__CALLLOCAL(func, boundary, op, debugger);
		    break;
		case CALLGLOBAL:
		    exec__CALLGLOBAL(func, boundary, op, debugger);
		    break;

		case BINOP:
		    exec__BINOP(func, boundary, op, debugger);
		    break;
		case UNOP:
		    exec__UNOP(func, boundary, op, debugger);
		    break;

		case INCL:
		    exec__INCL(func, boundary, op, debugger);
		    break;
		case DECL:
		    exec__DECL(func, boundary, op, debugger);
		    break;

		case INCLOSURE:
		    exec__INCLOSURE(func, boundary, op, debugger);
		    break;
		case ENDCLOSURE:
		    exec__ENDCLOSURE(func, boundary, op, debugger);
		    break;

		case NEWUPVAR:
		    exec__NEWUPVAR(func, boundary, op, debugger);
		    break;
		case NEWARRAY:
		    exec__NEWARRAY(func, boundary, op, debugger);
		    break;

		case SETLOCAL:
		    exec__SETLOCAL(func, boundary, op, debugger);
		    break;
		case SETGLOBAL:
		    exec__SETGLOBAL(func, boundary, op, debugger);
		    break;
		case SETLOCALARR:
		    exec__SETLOCALARR(func, boundary, op, debugger);
		    break;
		case SETGLOBALARR:
		    exec__SETGLOBALARR(func, boundary, op, debugger);
		    break;

		case GETLOCAL:
		    exec__GETLOCAL(func, boundary, op, debugger);
		    break;
		case GETGLOBAL:
		    exec__GETGLOBAL(func, boundary, op, debugger);
		    break;
		case GETLOCALARR:
		    exec__GETLOCALARR(func, boundary, op, debugger);
		    break;
		case GETGLOBALARR:
		    exec__GETGLOBALARR(func, boundary, op, debugger);
		    break;

		case DEBUG:
		    exec__DEBUG(func, boundary, op, debugger);
		    break;

		case TRY:
		    exec__TRY(func, boundary, op, debugger);
		    break;
		case CATCH:
		    exec__CATCH(func, boundary, op, debugger);
		    break;

		case RAISE:
		    exec__RAISE(func, boundary, op, debugger);
		    break;

		case CMP:
		    exec__CMP(func, boundary, op, debugger);
		    break;

		case BREAK:
		    if (debugger != null)
			debugger.breakpoint(this, func, op);
		    break;
	    }

	    boundary.programCounterValue++;

	    if (debugger != null)
		debugger.afterop(this, func, op);

	    if (!boundary.functionClosureStack.isEmpty() && boundary.functionClosureStack.size() > 1)
		if (boundary.programCounterValue > boundary.functionClosureStack.peek().endOfClosureAddress)
		    boundary.functionClosureStack.pop();
	}
    }

    private void __throw(String message) throws VMException {
	throw new VMException(instanceObj, this, message, null);
    }

    private <T> T __getoparg(Opcode o, int slot, Class<? extends T> klazz) throws VMException {
	Object obj = o.argobject(slot);
	if (!klazz.isAssignableFrom(obj.getClass()))
	    __throw(String.format("Opcode argument in slot %s is not of type %s", slot, klazz.getName()));
	return (T) obj;
    }

    private <T> T __getstackval(FunctionCallContext boundary, Class<? extends T> klazz) throws VMException {
	try {
	    Object obj = boundary.functionStackStack.pop();
	    if (!klazz.isAssignableFrom(obj.getClass()))
		__throw(String.format("Stack value %s is not of type %s", obj.getClass().getName(), klazz.getName()));
	    return (T) obj;
	} catch (EmptyStackException e) {
	    throw new VMException(instanceObj, this, "function stack empty", e);
	}
    }

    private <T> T __peekstackval(FunctionCallContext boundary, Class<? extends T> klazz) throws VMException {
	try {
	    Object obj = boundary.functionStackStack.peek();
	    if (!klazz.isAssignableFrom(obj.getClass()))
		__throw(String.format("Stack value %s is not of type %s", obj.getClass().getName(), klazz.getName()));
	    return (T) obj;
	} catch (EmptyStackException e) {
	    throw new VMException(instanceObj, this, "function stack empty", e);
	}
    }

    private Closure __getclosure(FunctionCallContext boundary, int local) throws VMException {
	try {
	    Closure closure = boundary.functionClosureStack.peek();
	    int index = boundary.functionClosureStack.size() - 1;
	    while (closure != null && closure.startOfClosureLocals > local)
		closure = boundary.functionClosureStack.get(--index);
	    return closure;
	} catch (EmptyStackException e) {
	    throw new VMException(instanceObj, this, "empty closure stack", e);
	}
    }

    private <T> T __getlocal(FunctionCallContext boundary, int local, Class<? extends T> klazz) throws VMException {
	Closure closure = __getclosure(boundary, local);
	if (closure == null)
	    __throw("cannot getlocal on a non-allocated lvar slot");
	local = local - closure.startOfClosureLocals;
	if (local < 0)
	    __throw("cannot getlocal on a non-allocated lvar slot");
	Object obj = closure.valuesOfClosureLocals.get(local);
	if (obj == null)
	    __throw(String.format("cannot dereference null local in slot %s", local));
	if (!klazz.isAssignableFrom(obj.getClass()))
	    __throw(String.format("Local value %s is not of type %s", obj.getClass().getName(), klazz.getName()));
	return (T) obj;
    }

    private VMType __getlocaltype(FunctionCallContext boundary, int local) throws VMException {
	Closure closure = __getclosure(boundary, local);
	if (closure == null)
	    __throw("cannot getlocaltype on a non-allocated lvar slot");
	local = local - closure.startOfClosureLocals;
	if (local < 0)
	    __throw("cannot getlocaltype on a non-allocated lvar slot");
	return closure.typesOfClosureLocals.get(local);
    }

    private BaseVMArrayImpl __getlocalarr(FunctionCallContext boundary, int local) throws VMException {
	Closure closure = __getclosure(boundary, local);
	if (closure == null)
	    __throw("cannot getlocalarr on a non-allocated lvar slot");
	local = local - closure.startOfClosureLocals;
	if (local < 0)
	    __throw("cannot getlocalarr on a non-allocated lvar slot");
	Object obj = closure.valuesOfClosureLocals.get(local);
	if (!(obj instanceof BaseVMArrayImpl))
	    __throw(String.format("Local value %s is not of array type", obj.getClass().getName()));
	return (BaseVMArrayImpl) obj;
    }

    private void __setlocal(VMFunction fn, FunctionCallContext boundary, int local, Object value) throws VMException {
	Closure closure = __getclosure(boundary, local);
	if (closure == null)
	    __throw("cannot setlocal on a non-allocated lvar slot");
	local = local - closure.startOfClosureLocals;
	if (local < 0)
	    __throw("cannot setlocal on a non-allocated lvar slot");
	VMType typeOfValue = instanceObj.gettype(fn.closure(), value);
	if (!instanceObj.checkcast(closure.typesOfClosureLocals.get(local), typeOfValue))
	    __throw(String.format("cannot set local in slot %s to type %s, expecting type %s", local,
		    typeOfValue, closure.typesOfClosureLocals.get(local)));
	closure.valuesOfClosureLocals.set(local, value);
    }

    private void __raise(VMDebugCallback debugger, FunctionCallContext boundary, Object raiseValue) throws VMException {
	if (callStack.isEmpty())
	    throw new UncaughtUserException(instanceObj, this, raiseValue);
	if (boundary != callStack.peek().b())
	    throw new VMException("wrong function context for raise call");
	if (boundary.functionTryCatchStack.isEmpty() && boundary.contextType != FunctionCallContextType.NATIVE) {
	    Pair<VMFunction, FunctionCallContext> obj = null;
	    while (!callStack.isEmpty() && (obj = callStack.peek()) != null) {
		if (obj.b().contextType == FunctionCallContextType.NATIVE || (obj.b().functionTryCatchStack != null && !obj.b().functionTryCatchStack.
			isEmpty()))
		    break;
		callStack.pop();
		if (debugger != null)
		    debugger.unwindfunc(this, (!callStack.isEmpty()) ? callStack.peek().a() : null, obj.a(), raiseValue);
	    }

	    if (callStack.isEmpty() || obj == null || obj.b().contextType == FunctionCallContextType.NATIVE)
		throw new UncaughtUserException(instanceObj, this, raiseValue);

	    final FunctionCallContext newBoundary = obj.b();

	    newBoundary.functionCaughtExceptionObj = raiseValue;
	    final Pair<Integer, Integer> functionTryCatchDest = newBoundary.functionTryCatchStack.peek();
	    newBoundary.programCounterValue = functionTryCatchDest.a() - 1;
	    while (newBoundary.functionClosureStack.size() > functionTryCatchDest.b())
		newBoundary.functionClosureStack.pop();
	} else {
	    boundary.functionCaughtExceptionObj = raiseValue;
	    final Pair<Integer, Integer> functionTryCatchDest = boundary.functionTryCatchStack.peek();
	    boundary.programCounterValue = functionTryCatchDest.a() - 1;
	    try {
		while (boundary.functionClosureStack.size() > functionTryCatchDest.b())
		    boundary.functionClosureStack.pop();
	    } catch (EmptyStackException e) {
		throw new VMException(instanceObj, this, "empty closure stack", e);
	    }
	}
    }

    private void exec__JMP(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	boolean flag = true;
	if ((o.getopts() & Opcodes.BITMASK_JMP_JCJT) != 0)
	    flag = boundary.comparisonRegisterValue;
	if ((o.getopts() & Opcodes.BITMASK_JMP_JCJF) != 0)
	    flag = !boundary.comparisonRegisterValue;
	if (flag) {
	    int offset = 0;
	    if ((o.getopts() & Opcodes.BITMASK_JMP_JMPS) != 0)
		offset = __getstackval(boundary, Integer.class);
	    else
		offset = (Integer) o.argobject(0);

	    if ((o.getopts() & Opcodes.BITMASK_JMP_AORO) != 0)
		boundary.programCounterValue = (offset - 1);
	    else
		boundary.programCounterValue += (offset - 1);

	    if (boundary.programCounterValue < 0) {
		__throw("JMP: program counter underflow");
		return;
	    }
	}
    }

    private void exec__FUNCPTR(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	String funcName = __getoparg(o, 0, String.class);
	VMFunctionPointer pointerObj = instanceObj.getfunc(func.closure(), funcName);
	if (pointerObj == null) {
	    __raise(debugger, boundary, String.format("No such function `%s`", funcName));
	    return;
	}
	boundary.functionStackStack.push(pointerObj);
    }

    private void exec__RETN(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	if (o.getopt(Opcodes.BITMASK_RETN_RVAL))
	    if (func.bytecode().maskflag(BytecodeFunctionDecl.FUNC_MASK_HASRETARR)) {
		BaseVMArrayImpl array = __getstackval(boundary, BaseVMArrayImpl.class);
		VMType returnType = func.closure().types().getTypeOf(func.bytecode().returntype());
		VMType hasType = array.typeOfArray;
		if (!instanceObj.checkcast(returnType, hasType))
		    __raise(debugger, boundary, String.format("expected %s array return type, got %s array type", returnType, hasType));
		exitfunc(debugger, func, array);
	    } else {
		Object any = __getstackval(boundary, Object.class);
		if (any instanceof BaseVMArrayImpl)
		    __raise(debugger, boundary, "unexpected array return from literal return function");
		VMType returnType = func.closure().types().getTypeOf(func.bytecode().returntype());
		VMType hasType = instanceObj.gettype(func.closure(), any);
		if (!instanceObj.checkcast(returnType, hasType))
		    __raise(debugger, boundary, String.format("expected %s return type, got %s type", returnType, hasType));
		exitfunc(debugger, func, any);
	    }
	else
	    exitfunc(debugger, func, null);
    }

    private void exec__YIELD(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	if (o.getopt(Opcodes.BITMASK_YIELD_STACK)) {
	    boundary.functionYieldObj = __getstackval(boundary, Object.class);
	    yielded = true;
	} else {
	    boundary.functionYieldObj = null;
	    yielded = true;
	}
    }

    private void exec__CALL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	String fnName = __getoparg(o, 0, String.class);
	int numArgs = __getoparg(o, 1, Integer.class);
	Object[] argvals = new Object[numArgs];
	VMFunctionPointer ptr = instanceObj.getfunc(func.closure(), fnName);
	if (ptr == null) {
	    __raise(debugger, boundary, String.format("No such function %s for function call", fnName));
	    return;
	}
	for (int i = (numArgs - 1); i >= 0; i--)
	    argvals[i] = __getstackval(boundary, Object.class);
	boundary.desiresCallResult = o.getopt(Opcodes.BITMASK_CALL_RVAL);
	callfunc(debugger, ptr, argvals, o);
    }

    private void exec__CALLLOCAL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int localNum = __getoparg(o, 0, Integer.class);
	int numArgs = __getoparg(o, 1, Integer.class);
	Object[] argvals = new Object[numArgs];
	for (int i = (numArgs - 1); i >= 0; i--)
	    argvals[i] = __getstackval(boundary, Object.class);
	VMFunctionPointer ptr = __getlocal(boundary, localNum, VMFunctionPointer.class);
	if (ptr == null) {
	    __raise(debugger, boundary, String.format("Attempt to dereference nothing value local %s for function call", localNum));
	    return;
	}
	boundary.desiresCallResult = o.getopt(Opcodes.BITMASK_CALLLOCAL_RVAL);
	callfunc(debugger, ptr, argvals, o);
    }

    private void exec__CALLGLOBAL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	String globalName = __getoparg(o, 0, String.class);
	int numArgs = __getoparg(o, 1, Integer.class);
	Object[] argvals = new Object[numArgs];
	for (int i = (numArgs - 1); i >= 0; i--)
	    argvals[i] = __getstackval(boundary, Object.class);

	GlobalBox globalBox = instanceObj.getglobal(func.closure(), globalName);
	if (globalBox == null)
	    __throw(String.format("CALLGLOBAL: no such global %s", globalName));

	if (instanceObj.checkcast(BaseVMPrimitiveType.TYPE_CODE, globalBox.typeOfGlobalValue))
	    __throw(String.format("CALLGLOBAL: global not code type"));

	VMFunctionPointer ptr = (VMFunctionPointer) globalBox.valueInGlobalBox;

	boundary.desiresCallResult = o.getopt(Opcodes.BITMASK_CALLGLOBAL_RVAL);
	callfunc(debugger, ptr, argvals, o);
    }

    private void exec__BINOP(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int opcode = __getoparg(o, 0, Integer.class);
	BinaryExpressionParamValueTable asEnum = BinaryExpressionParamValueTable.get(opcode);
	switch (asEnum) {

	    case BIT_AND:
	    case BIT_OR:
	    case BIT_SHL:
	    case BIT_SHR:
	    case BIT_XOR: {
		int rhs = __getstackval(boundary, Integer.class);
		int lhs = __getstackval(boundary, Integer.class);
		int product = 0;
		switch (asEnum) {
		    case BIT_AND:
			product = lhs & rhs;
			break;
		    case BIT_OR:
			product = lhs | rhs;
			break;
		    case BIT_SHL:
			product = lhs << rhs;
			break;
		    case BIT_SHR:
			product = lhs >> rhs;
			break;
		    case BIT_XOR:
			product = lhs ^ rhs;
			break;
		    default:
			throw new IllegalStateException();
		}
		boundary.functionStackStack.push(product);
	    }
	    break;

	    case BOOL_AND:
	    case BOOL_OR: {
		boolean rhs = __getstackval(boundary, Boolean.class);
		boolean lhs = __getstackval(boundary, Boolean.class);
		boolean product = false;
		switch (asEnum) {
		    case BOOL_AND:
			product = lhs & rhs;
			break;
		    case BOOL_OR:
			product = lhs | rhs;
			break;
		    default:
			throw new IllegalStateException();
		}
		boundary.functionStackStack.push(product);
	    }
	    break;

	    case NUM_ADD:
	    case NUM_SUB:
	    case NUM_MUL:
	    case NUM_DIV: {
		Object rhs = __getstackval(boundary, Object.class);
		Object lhs = __getstackval(boundary, Object.class);

		if (!(rhs instanceof Integer) && !(rhs instanceof Double) && !(rhs instanceof Long)) {
		    __raise(debugger, boundary, String.format("Binary operation right-hand-side must be numeric type"));
		    return;
		}
		if (!(lhs instanceof Integer) && !(lhs instanceof Double) && !(rhs instanceof Long)) {
		    __raise(debugger, boundary, String.format("Binary operation left-hand-side must be numeric type"));
		    return;
		}

		Integer boxIntRhs = null, boxIntLhs = null;
		Double boxDblRhs = null, boxDblLhs = null;
		Long boxLngRhs = null, boxLngLhs = null;

		if (rhs instanceof Long)
		    boxLngRhs = (Long) rhs;
		else if (rhs instanceof Integer)
		    boxIntRhs = (Integer) rhs;
		else
		    boxDblRhs = (Double) rhs;

		if (lhs instanceof Long)
		    boxLngLhs = (Long) lhs;
		else if (lhs instanceof Integer)
		    boxIntLhs = (Integer) lhs;
		else
		    boxDblLhs = (Double) lhs;

		Object product = null;

		switch (asEnum) {
		    case NUM_ADD:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs + boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (int) (boxIntLhs + boxIntRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (double) (boxDblLhs + boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (double) (boxIntLhs + boxDblRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (double) (boxDblLhs + boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    case NUM_SUB:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs - boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (int) (boxIntLhs - boxIntRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (double) (boxDblLhs - boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (double) (boxIntLhs - boxDblRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (double) (boxDblLhs - boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    case NUM_MUL:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs * boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (int) (boxIntLhs * boxIntRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (double) (boxDblLhs * boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (double) (boxIntLhs * boxDblRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (double) (boxDblLhs * boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    case NUM_DIV:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs / boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (int) (boxIntLhs / boxIntRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (int) (boxDblLhs / boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (double) (boxIntLhs / boxDblRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (double) (boxDblLhs / boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    default:
			throw new IllegalStateException();
		}

		boundary.functionStackStack.push(product);
	    }
	    break;

	    case NUM_MOD:
	    case NUM_POW: {

		int rhs = __getstackval(boundary, Integer.class);
		Object lhs = __getstackval(boundary, Object.class);

		if (!(lhs instanceof Integer) && !(lhs instanceof Double) && !(lhs instanceof Long)) {
		    __raise(debugger, boundary, "Binary operation left-hand-side must be numeric type");
		    return;
		}

		Integer boxIntLhs = null;
		Double boxDblLhs = null;
		Long boxLngLhs = null;

		if (lhs instanceof Long)
		    boxLngLhs = (Long) lhs;
		else if (lhs instanceof Integer)
		    boxIntLhs = (Integer) lhs;
		else
		    boxDblLhs = (Double) lhs;

		Object product = null;
		switch (asEnum) {
		    case NUM_MOD:
			if (boxLngLhs != null)
			    product = boxLngLhs % rhs;
			else if (boxIntLhs != null)
			    product = boxIntLhs % rhs;
			else
			    product = boxDblLhs % rhs;
			break;
		    case NUM_POW:
			if (boxLngLhs != null)
			    product = (long) Math.pow(boxLngLhs, rhs);
			else if (boxIntLhs != null)
			    product = (int) Math.pow(boxIntLhs, rhs);
			else
			    product = (double) Math.pow(boxDblLhs, rhs);
			break;
		    default:
			throw new IllegalStateException();
		}

		boundary.functionStackStack.push(product);
	    }
	    break;

	    case VAL_EQ:
	    case VAL_NEQ: {
		Object rhs = __getstackval(boundary, Object.class);
		Object lhs = __getstackval(boundary, Object.class);

		if (lhs instanceof Integer || lhs instanceof Double || lhs instanceof Long
			|| rhs instanceof Integer || rhs instanceof Double || rhs instanceof Long) {
		    Boolean product = null;
		    if (lhs instanceof Long && rhs instanceof Long)
			switch (asEnum) {
			    case VAL_EQ:
				product = (long) lhs == (long) rhs;
				break;
			    case VAL_NEQ:
				product = (long) lhs != (long) rhs;
				break;
			    default:
				throw new IllegalStateException();
			}
		    else if (lhs instanceof Integer && rhs instanceof Integer)
			switch (asEnum) {
			    case VAL_EQ:
				product = (int) lhs == (int) rhs;
				break;
			    case VAL_NEQ:
				product = (int) lhs != (int) rhs;
				break;
			    default:
				throw new IllegalStateException();
			}
		    else if (lhs instanceof Double && rhs instanceof Double)
			switch (asEnum) {
			    case VAL_EQ:
				product = (double) lhs == (double) rhs;
				break;
			    case VAL_NEQ:
				product = (double) lhs != (double) rhs;
				break;
			    default:
				throw new IllegalStateException();
			}
		    else {
			__raise(debugger, boundary, String.format("Binary comparison operation right-hand-side must be numeric type"));
			return;
		    }
		    boundary.functionStackStack.push(product);
		} else if (lhs instanceof String && rhs instanceof String)
		    boundary.functionStackStack.push(((String) lhs).equals((String) rhs));
		else {
		    Boolean product = null;
		    switch (asEnum) {
			case VAL_EQ:
			    product = (lhs.equals(rhs));
			    break;
			case VAL_NEQ:
			    product = !(lhs.equals(rhs));
			    break;
			default:
			    throw new IllegalStateException();
		    }
		    boundary.functionStackStack.push(product);
		}
	    }
	    break;

	    case VAL_GT:
	    case VAL_GTEQ:
	    case VAL_LT:
	    case VAL_LTEQ: {
		Object rhs = __getstackval(boundary, Object.class);
		Object lhs = __getstackval(boundary, Object.class);

		if (!(rhs instanceof Integer) && !(rhs instanceof Double) && !(rhs instanceof Long)) {
		    __raise(debugger, boundary, String.format("Binary operation right-hand-side must be numeric type"));
		    return;
		}
		if (!(lhs instanceof Integer) && !(lhs instanceof Double) && !(rhs instanceof Long)) {
		    __raise(debugger, boundary, String.format("Binary operation left-hand-side must be numeric type"));
		    return;
		}

		Integer boxIntRhs = null, boxIntLhs = null;
		Double boxDblRhs = null, boxDblLhs = null;
		Long boxLngRhs = null, boxLngLhs = null;

		if (rhs instanceof Long)
		    boxLngRhs = (Long) rhs;
		else if (rhs instanceof Integer)
		    boxIntRhs = (Integer) rhs;
		else
		    boxDblRhs = (Double) rhs;

		if (lhs instanceof Long)
		    boxLngLhs = (Long) lhs;
		else if (lhs instanceof Integer)
		    boxIntLhs = (Integer) lhs;
		else
		    boxDblLhs = (Double) lhs;

		Boolean product = null;

		switch (asEnum) {
		    case VAL_GT:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs > boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (boxIntLhs > boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (boxIntLhs > boxDblRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (boxDblLhs > boxIntRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (boxDblLhs > boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    case VAL_GTEQ:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs >= boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (boxIntLhs >= boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (boxIntLhs >= boxDblRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (boxDblLhs >= boxIntRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (boxDblLhs >= boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    case VAL_LT:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs < boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (boxIntLhs < boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (boxIntLhs < boxDblRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (boxDblLhs < boxIntRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (boxDblLhs < boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    case VAL_LTEQ:
			if (boxLngLhs != null || boxLngRhs != null) {
			    if (boxLngLhs == null || boxLngRhs == null)
				__raise(debugger, boundary, "Binary operation on long values requires both values be long type");
			    product = boxLngLhs <= boxLngRhs;
			} else if (boxIntLhs != null && boxIntRhs != null)
			    product = (boxIntLhs <= boxIntRhs);
			else if (boxIntLhs != null && boxDblRhs != null)
			    product = (boxIntLhs <= boxDblRhs);
			else if (boxDblLhs != null && boxIntRhs != null)
			    product = (boxDblLhs <= boxIntRhs);
			else if (boxDblLhs != null && boxDblRhs != null)
			    product = (boxDblLhs <= boxDblRhs);
			else
			    throw new IllegalStateException();
			break;
		    default:
			throw new IllegalStateException();
		}

		boundary.functionStackStack.push(product);
	    }
	    break;

	    case ILLEGAL:
		for (int i = 0; i < 2; i++)
		    __getstackval(boundary, Object.class);
		break;
	    default:
		throw new IllegalStateException();
	}
    }

    private void exec__UNOP(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int opcode = __getoparg(o, 0, Integer.class);
	UnaryExpressionParamValueTable asEnum = UnaryExpressionParamValueTable.get(opcode);
	switch (asEnum) {
	    case BIT_NEGATE:
		int word = __getstackval(boundary, Integer.class);
		boundary.functionStackStack.push(~word);
		break;

	    case BOOL_NOT:
		boolean bool = __getstackval(boundary, Boolean.class);
		boundary.functionStackStack.push(!bool);
		break;

	    case NUM_ABSVAL:
	    case NUM_NEGATE: {
		Object rhs = __getstackval(boundary, Object.class);
		if (rhs instanceof Integer)
		    switch (asEnum) {
			case NUM_ABSVAL:
			    boundary.functionStackStack.push(Math.abs((Integer) rhs));
			    break;
			case NUM_NEGATE:
			    boundary.functionStackStack.push(-(Integer) rhs);
			    break;
			default:
			    throw new IllegalStateException();
		    }
		else if (rhs instanceof Double)
		    switch (asEnum) {
			case NUM_ABSVAL:
			    boundary.functionStackStack.push(Math.abs((Double) rhs));
			    break;
			case NUM_NEGATE:
			    boundary.functionStackStack.push(-(Double) rhs);
			    break;
			default:
			    throw new IllegalStateException();
		    }
		else {
		    __raise(debugger, boundary, String.format("Unsupported type %s for numerical unary operation", rhs.getClass().getName()));
		    return;
		}
	    }
	    break;
	    case ILLEGAL:
		__getstackval(boundary, Object.class);
		break;

	    default:
		throw new IllegalStateException();
	}
    }

    private void exec__INCL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int local = __getoparg(o, 0, Integer.class);
	VMType localType = __getlocaltype(boundary, local);
	if (localType != BaseVMPrimitiveType.TYPE_INTEGER && localType != BaseVMPrimitiveType.TYPE_REAL && localType != BaseVMPrimitiveType.TYPE_LONG)
	    __raise(debugger, boundary, String.format("Increment not permitted on non-numeric type %s", localType));

	if (localType == BaseVMPrimitiveType.TYPE_INTEGER) {
	    int value = __getlocal(boundary, local, Integer.class);
	    if (o.getopt(Opcodes.BITMASK_INCL_PRE))
		boundary.functionStackStack.push(value);
	    value++;
	    if (o.getopt(Opcodes.BITMASK_INCL_POST))
		boundary.functionStackStack.push(value);
	    __setlocal(func, boundary, local, value);
	} else if (localType == BaseVMPrimitiveType.TYPE_REAL) {
	    double value = __getlocal(boundary, local, Double.class);
	    if (o.getopt(Opcodes.BITMASK_INCL_PRE))
		boundary.functionStackStack.push(value);
	    value++;
	    if (o.getopt(Opcodes.BITMASK_INCL_POST))
		boundary.functionStackStack.push(value);
	    __setlocal(func, boundary, local, value);
	} else if ( localType == BaseVMPrimitiveType.TYPE_LONG) {
	    long value = __getlocal(boundary, local, Long.class);
	    if (o.getopt(Opcodes.BITMASK_INCL_PRE))
		boundary.functionStackStack.push(value);
	    value++;
	    if (o.getopt(Opcodes.BITMASK_INCL_POST))
		boundary.functionStackStack.push(value);
	    __setlocal(func, boundary, local, value);
	} else
	    __throw("unsupported type for increment operation");
    }

    private void exec__DECL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int local = __getoparg(o, 0, Integer.class);
	VMType localType = __getlocaltype(boundary, local);
	if (localType != BaseVMPrimitiveType.TYPE_INTEGER && localType != BaseVMPrimitiveType.TYPE_REAL && localType != BaseVMPrimitiveType.TYPE_LONG)
	    __raise(debugger, boundary, String.format("Decrement not permitted on non-numeric type %s", localType));

	if (localType == BaseVMPrimitiveType.TYPE_INTEGER) {
	    int value = __getlocal(boundary, local, Integer.class);
	    if (o.getopt(Opcodes.BITMASK_DECL_PRE))
		boundary.functionStackStack.push(value);
	    value--;
	    if (o.getopt(Opcodes.BITMASK_DECL_POST))
		boundary.functionStackStack.push(value);
	    __setlocal(func, boundary, local, value);
	} else if (localType == BaseVMPrimitiveType.TYPE_REAL) {
	    double value = __getlocal(boundary, local, Double.class);
	    if (o.getopt(Opcodes.BITMASK_DECL_PRE))
		boundary.functionStackStack.push(value);
	    value--;
	    if (o.getopt(Opcodes.BITMASK_DECL_POST))
		boundary.functionStackStack.push(value);
	    __setlocal(func, boundary, local, value);
	} else if (localType == BaseVMPrimitiveType.TYPE_LONG) {
	    long value = __getlocal(boundary, local, Long.class);
	    if (o.getopt(Opcodes.BITMASK_DECL_PRE))
		boundary.functionStackStack.push(value);
	    value--;
	    if (o.getopt(Opcodes.BITMASK_DECL_POST))
		boundary.functionStackStack.push(value);
	    __setlocal(func, boundary, local, value);
	} else
	    __throw("unsupported type for decrement operation");
    }

    private void exec__INCLOSURE(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	Closure lastClosure = boundary.functionClosureStack.peek();
	Closure newClosure = new Closure();
	newClosure.endOfClosureAddress = __getoparg(o, 0, Integer.class);
	newClosure.startOfClosureLocals = lastClosure.startOfClosureLocals + lastClosure.numberOfClosureLocals;
	newClosure.numberOfClosureLocals = 0;
	newClosure.typesOfClosureLocals = new LinkedList<>();
	newClosure.valuesOfClosureLocals = new LinkedList<>();
	boundary.functionClosureStack.push(newClosure);
    }

    private void exec__ENDCLOSURE(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	try {
	    Closure exitClosure = boundary.functionClosureStack.pop();
	    if (exitClosure == callStack.peek().b().functionClosureObj)
		__throw("ENDCLOSURE: exited the function root closure");
	    if (boundary.functionClosureStack.isEmpty())
		__throw("ENDCLOSURE: the function closure stack is empty");
	} catch (EmptyStackException e) {
	    throw new VMException(instanceObj, this, "empty closure stack", e);
	}
    }

    private void exec__NEWUPVAR(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	try {
	    Closure currentClosure = boundary.functionClosureStack.peek();
	    if (currentClosure.equals(boundary.functionClosureObj))
		__throw("NEWUPVAR: cannot define new upvar in non-closure scope");
	    int ptr = currentClosure.startOfClosureLocals + currentClosure.numberOfClosureLocals;
	    currentClosure.numberOfClosureLocals++;
	    if (ptr != __getoparg(o, 1, Integer.class))
		__throw(String.format("NEWUPVAR: new upvar defined in slot %s does not match expected lvar slot %s", ptr, __getoparg(o, 1,
			Integer.class)));

	    final VMType typeOfNewUpvar = func.closure().types().getTypeOf(__getoparg(o, 0, Integer.class));
	    currentClosure.typesOfClosureLocals.add(typeOfNewUpvar);

	    if (o.getopt(Opcodes.BITMASK_NEWUPVAR_ARRAY)) {
		BaseVMArrayImpl allocArrayBox = new BaseVMArrayImpl(typeOfNewUpvar, __getstackval(boundary, Integer.class));
		currentClosure.valuesOfClosureLocals.add(allocArrayBox);
	    } else
		currentClosure.valuesOfClosureLocals.add(null);
	} catch (EmptyStackException e) {
	    throw new VMException(instanceObj, this, "empty closure stack", e);
	}
    }

    private void exec__NEWARRAY(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	try {
	    Closure currentClosure = boundary.functionClosureStack.peek();
	    if (!currentClosure.equals(boundary.functionClosureObj))
		__throw("NEWARRAY: cannot initialize array in closure scope");

	    int sizeObj = __getstackval(boundary, Integer.class);
	    VMType type = func.closure().types().getTypeOf(__getoparg(o, 0, Integer.class));
	    int localNum = __getoparg(o, 1, Integer.class);

	    if (currentClosure.typesOfClosureLocals.get(localNum) != type)
		__throw("NEWARRAY: cannot initialize array of type to other type");
	    if (!currentClosure.arrayTypeOfClosureLocal.get(localNum))
		__throw("NEWARRAY: cannot initialize array into non-array local slot");

	    BaseVMArrayImpl allocArrayBox = new BaseVMArrayImpl(currentClosure.typesOfClosureLocals.get(localNum), sizeObj);
	    currentClosure.valuesOfClosureLocals.set(localNum, allocArrayBox);
	} catch (EmptyStackException e) {
	    throw new VMException(instanceObj, this, "empty closure stack", e);
	}
    }

    private void exec__SETLOCAL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	__setlocal(func, boundary, __getoparg(o, 0, Integer.class), __getstackval(boundary, Object.class));
    }

    private void exec__SETGLOBAL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	String globalName = __getoparg(o, 0, String.class);
	GlobalBox globalBox = instanceObj.getglobal(func.closure(), globalName);
	if (globalBox == null)
	    throw new IllegalStateException(String.format("SETGLOBAL: no such global %s", globalName));

	if (globalBox.typeOfGlobal == GlobalType.NATIVEGLOBAL)
	    throw new IllegalStateException("SETGLOBAL: cannot set native global");

	Object newValueInBox = __getstackval(boundary, Object.class);

	if (newValueInBox instanceof BaseVMArrayImpl) {
	    if (!globalBox.globalIsArrayType)
		throw new IllegalStateException(String.format("SETGLOBAL: cannot set literal global to array value"));

	    BaseVMArrayImpl assignArrayObj = (BaseVMArrayImpl) newValueInBox;
	    if (assignArrayObj.typeOfArray != globalBox.typeOfGlobalValue)
		throw new IllegalStateException(String.format("SETGLOBAL: global with array-type %s cannot contain array-value of type %s",
			globalBox.typeOfGlobalValue, assignArrayObj.typeOfArray));
	    globalBox.valueInGlobalBox = newValueInBox;
	} else {
	    VMType typeOfNewValueInBox = instanceObj.gettype(func.closure(), newValueInBox);
	    if (typeOfNewValueInBox != globalBox.typeOfGlobalValue)
		throw new IllegalStateException(String.format("SETGLOBAL: global with type %s cannot contain value of type %s",
			globalBox.typeOfGlobalValue, typeOfNewValueInBox));
	    globalBox.valueInGlobalBox = newValueInBox;
	}

    }

    private void exec__SETLOCALARR(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int arrayIdx = __getstackval(boundary, Integer.class);
	Object assignVal = __getstackval(boundary, Object.class);
	int localNum = __getoparg(o, 0, Integer.class);

	BaseVMArrayImpl arrayPtr = __getlocalarr(boundary, localNum);

	if (arrayPtr == null) {
	    __raise(debugger, boundary, "array not initialized");
	    return;
	}
	if (0 > arrayIdx || arrayIdx >= arrayPtr.sizeOfArray) {
	    __raise(debugger, boundary, String.format("array index %s out of bounds", arrayIdx));
	    return;
	}

	VMType typeOfAssignVal = instanceObj.gettype(func.closure(), assignVal);
	if (typeOfAssignVal != arrayPtr.typeOfArray) {
	    __raise(debugger, boundary, String.format("array type %s cannot contain values of type %s", arrayPtr.typeOfArray,
		    typeOfAssignVal));
	    return;
	}
	arrayPtr.arrayValues[arrayIdx] = assignVal;
    }

    private void exec__SETGLOBALARR(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int arrayIdx = __getstackval(boundary, Integer.class);
	Object assignVal = __getstackval(boundary, Object.class);
	String globalName = __getoparg(o, 0, String.class);

	GlobalBox globalBox = instanceObj.getglobal(func.closure(), globalName);
	if (globalBox == null)
	    __throw(String.format("SETGLOBALARR: no such global %s", globalName));

	if (globalBox.typeOfGlobal == GlobalType.NATIVEGLOBAL)
	    __throw(String.format("SETGLOBALARR: cannot set native global"));

	if (!globalBox.globalIsArrayType || !(globalBox.valueInGlobalBox instanceof BaseVMArrayImpl))
	    throw new IllegalStateException(String.format("GETGLOBALARR: global %s is not array type", globalName));

	BaseVMArrayImpl arrayPtr = (BaseVMArrayImpl) globalBox.valueInGlobalBox;

	if (arrayPtr == null) {
	    __raise(debugger, boundary, "array not initialized");
	    return;
	}
	if (0 > arrayIdx || arrayIdx >= arrayPtr.sizeOfArray) {
	    __raise(debugger, boundary, String.format("array index %s out of bounds", arrayIdx));
	    return;
	}

	VMType typeOfAssignVal = instanceObj.gettype(func.closure(), assignVal);
	if (typeOfAssignVal != arrayPtr.typeOfArray) {
	    __raise(debugger, boundary, String.format("array type %s cannot contain values of type %s", arrayPtr.typeOfArray,
		    typeOfAssignVal));
	    return;
	}
	arrayPtr.arrayValues[arrayIdx] = assignVal;

    }

    private void exec__GETLOCAL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int local = __getoparg(o, 0, Integer.class);
	VMType localType = __getlocaltype(boundary, local);
	Object value = __getlocal(boundary, local, Object.class);
	if ((localType == BaseVMPrimitiveType.TYPE_HANDLE || localType instanceof BaseVMUserType) && value == null) {
	    __raise(debuggerObj, boundary, "attempt to dereference null handle pointer");
	    return;
	}
	boundary.functionStackStack.push(value);
    }

    private void exec__GETGLOBAL(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	String globalName = __getoparg(o, 0, String.class);
	GlobalBox globalBox = instanceObj.getglobal(func.closure(), globalName);
	if (globalBox == null)
	    throw new IllegalStateException(String.format("GETGLOBAL: no such global %s", globalName));

	if ((globalBox.typeOfGlobalValue == BaseVMPrimitiveType.TYPE_HANDLE || globalBox.typeOfGlobalValue instanceof BaseVMUserType) && globalBox.valueInGlobalBox == null) {
	    __raise(debuggerObj, boundary, "attempt to dereference null handle pointer");
	    return;
	}
	boundary.functionStackStack.push(globalBox.valueInGlobalBox);
    }

    private void exec__GETLOCALARR(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	int arrayIdx = __getstackval(boundary, Integer.class);
	int localNum = __getoparg(o, 0, Integer.class);
	BaseVMArrayImpl arrayPtr = __getlocalarr(boundary, localNum);
	if (arrayPtr == null) {
	    __raise(debugger, boundary, "array not initialized");
	    return;
	}
	if (0 > arrayIdx || arrayIdx >= arrayPtr.sizeOfArray) {
	    __raise(debugger, boundary, "array index out of bounds");
	    return;
	}
	boundary.functionStackStack.push(arrayPtr.arrayValues[arrayIdx]);
    }

    private void exec__GETGLOBALARR(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	String globalName = __getoparg(o, 0, String.class);
	int globalArrayIndex = __getstackval(boundary, Integer.class);

	GlobalBox globalBox = instanceObj.getglobal(func.closure(), globalName);
	if (globalBox == null)
	    throw new IllegalStateException(String.format("GETGLOBALARR: no such global %s", globalName));

	if (!globalBox.globalIsArrayType || !(globalBox.valueInGlobalBox instanceof BaseVMArrayImpl))
	    throw new IllegalStateException(String.format("GETGLOBALARR: global %s is not array type", globalName));
	BaseVMArrayImpl globalArrayObj = (BaseVMArrayImpl) globalBox.valueInGlobalBox;
	if ((globalBox.typeOfGlobalValue == BaseVMPrimitiveType.TYPE_HANDLE || globalBox.typeOfGlobalValue instanceof BaseVMUserType)
		&& globalArrayObj.arrayValues[globalArrayIndex] == null) {
	    __raise(debuggerObj, boundary, "attempt to dereference null handle pointer");
	    return;
	}
	boundary.functionStackStack.push(globalArrayObj.arrayValues[globalArrayIndex]);
    }

    private void exec__DEBUG(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	throw new UnsupportedOperationException("DEBUG");
    }

    private void exec__TRY(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	try {
	    boundary.functionTryCatchStack.push(new Pair<>(__getoparg(o, 0, Integer.class), boundary.functionClosureStack.size()));
	} catch (EmptyStackException e) {
	    throw new VMException(instanceObj, this, "empty closure stack", e);
	}
    }

    private void exec__CATCH(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	if (!o.getopt(Opcodes.BITMASK_CATCH_HASNEXT))
	    boundary.functionTryCatchStack.pop();

	VMType catchtype = func.closure().types().getTypeOf(__getoparg(o, 0, Integer.class));
	int catchnext = __getoparg(o, 1, Integer.class);

	if (boundary.functionCaughtExceptionObj == null)
	    boundary.programCounterValue = (catchnext - 1);
	else {
	    VMType objtype = instanceObj.gettype(func.closure(), boundary.functionCaughtExceptionObj);
	    if (instanceObj.checkcast(catchtype, objtype)) {
		boundary.functionStackStack.push(boundary.functionCaughtExceptionObj);
		boundary.functionCaughtExceptionObj = null;
	    } else if (o.getopt(Opcodes.BITMASK_CATCH_HASNEXT))
		boundary.programCounterValue = (catchnext - 1);
	    else
		__raise(debugger, boundary, boundary.functionCaughtExceptionObj);
	}
    }

    private void exec__RAISE(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	__raise(debugger, boundary, __getstackval(boundary, Object.class));
    }

    private void exec__CMP(VMFunction func, FunctionCallContext boundary, Opcode o, VMDebugCallback debugger) throws VMException {
	Object value = null;
	if (o.getopt(Opcodes.BITMASK_CMP_CONSUME))
	    value = __getstackval(boundary, Object.class);
	else
	    value = __peekstackval(boundary, Object.class);
	if (!(value instanceof Boolean)) {
	    __raise(debugger, boundary, String.format("Cannot set compare register from non-boolean type"));
	    return;
	}
	boundary.comparisonRegisterValue = (boolean) value;
    }

}
