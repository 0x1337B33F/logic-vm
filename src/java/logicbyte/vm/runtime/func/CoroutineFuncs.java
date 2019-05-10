package logicbyte.vm.runtime.func;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMFunctionPointer;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.exception.UncaughtUserException;
import logicbyte.vm.exception.VMException;
import logicbyte.vm.runtime.BaseVMJFunctionImpl;
import logicbyte.vm.VMJFunctionLambda;
import logicbyte.vm.exception.ThrowObjectException;

/**
 *
 * @author Lochie
 */
public class CoroutineFuncs {

    public static class CoroutineImpl {

	VMFunctionPointer funcPtr;
	VMInvokable callableObj;

	boolean complete = false;
	Object result = null;

	Exception caughtExceptionObj;

    }

    abstract static class CoroutineFuncInternalTyped implements VMJFunctionLambda {

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException, Exception {
	    if (args.length == 0)
		throw new ThrowObjectException("arguments required");
	    if (!(args[0] instanceof CoroutineImpl))
		throw new ThrowObjectException("argument 1 not coroutine");
	    CoroutineImpl co = (CoroutineImpl) args[0];
	    return tailcall(vm, executor, debug, co, args);
	}

	public abstract Object tailcall(LogicVM vm, VMInvokable executor, VMDebugCallback debug, CoroutineImpl co, Object[] args) throws ThrowObjectException, Exception;
    }

    static class CoroutineFuncInternalReturnTyped<R> implements VMJFunctionLambda {

	final Class<R> theTypeOfR;

	public CoroutineFuncInternalReturnTyped(Class<R> theTypeOfR) {
	    this.theTypeOfR = theTypeOfR;
	}

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException, Exception {
	    if (args.length == 0)
		throw new ThrowObjectException("arguments required");
	    if (!(args[0] instanceof CoroutineImpl))
		throw new ThrowObjectException("argument 1 not coroutine");
	    CoroutineImpl co = (CoroutineImpl) args[0];
	    if (co.complete) {
		Object resultObj = vm.result(co.callableObj);
		if (resultObj == null || !theTypeOfR.isAssignableFrom(resultObj.getClass()))
		    throw new ThrowObjectException(String.format("result of coroutine not of type %s", theTypeOfR.getName()));
		return theTypeOfR.cast(resultObj);
	    } else {
		Object yieldObj = vm.yield(co.callableObj);
		if (yieldObj == null || !theTypeOfR.isAssignableFrom(yieldObj.getClass()))
		    throw new ThrowObjectException(String.format("yield of coroutine not of type %s", theTypeOfR.getName()));
		return theTypeOfR.cast(yieldObj);
	    }
	}
    }

    public static void apply(LogicVM inst) throws NoSuchMethodException {

	inst.defineJType("coroutine", CoroutineImpl.class);

	inst.defineJFunc("Coroutine_Create", BaseVMJFunctionImpl.generateFunctionLambda((vm, exec, debug, args) -> {
	    if (args.length == 0)
		throw new ThrowObjectException("arguments required");
	    if (!(args[0] instanceof VMFunctionPointer))
		throw new ThrowObjectException("argument 1 not code");

	    VMFunctionPointer ptr = (VMFunctionPointer) args[0];
	    final VMInvokable invoke = vm.invoke(debug, ptr);

	    final CoroutineImpl allocCoObj = new CoroutineImpl();
	    allocCoObj.funcPtr = ptr;
	    allocCoObj.callableObj = invoke;

	    invoke.invoke(debug, null);

	    return allocCoObj;
	}));

	inst.defineJFunc("Coroutine_Step", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalTyped() {
	    @Override
	    public Object tailcall(LogicVM vm, VMInvokable executor, VMDebugCallback debug, CoroutineImpl co, Object[] args) throws ThrowObjectException, Exception {
		if (co.complete)
		    return true;

		try {
		    while (true)
			if (co.callableObj.advance(debug)) {
			    if (!vm.yielded(co.callableObj)) {
				co.complete = true;
				co.result = vm.result(co.callableObj);
			    }
			    break;
			}
		} catch (VMException e) {
		    co.complete = true;
		    co.caughtExceptionObj = e;
		}

		return co.complete;
	    }
	}));

	inst.defineJFunc("Coroutine_IsComplete", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalTyped() {
	    @Override
	    public Object tailcall(LogicVM vm, VMInvokable executor, VMDebugCallback debug, CoroutineImpl co, Object[] args) throws ThrowObjectException, Exception {
		return co.complete;
	    }
	}));

	inst.defineJFunc("Coroutine_IsError", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalTyped() {
	    @Override
	    public Object tailcall(LogicVM vm, VMInvokable executor, VMDebugCallback debug, CoroutineImpl co, Object[] args) throws ThrowObjectException, Exception {
		return co.caughtExceptionObj != null;
	    }
	}));

	inst.defineJFunc("Coroutine_ThrowError", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalTyped() {
	    @Override
	    public Object tailcall(LogicVM vm, VMInvokable executor, VMDebugCallback debug, CoroutineImpl co, Object[] args) throws ThrowObjectException, Exception {
		if (co.caughtExceptionObj == null)
		    return null;
		throw co.caughtExceptionObj;
	    }
	}));

	inst.defineJFunc("Coroutine_GetError", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalTyped() {
	    @Override
	    public Object tailcall(LogicVM vm, VMInvokable executor, VMDebugCallback debug, CoroutineImpl co, Object[] args) throws ThrowObjectException, Exception {
		if (co.caughtExceptionObj == null)
		    return null;
		if (co.caughtExceptionObj instanceof ThrowObjectException)
		    return ((ThrowObjectException) co.caughtExceptionObj).getThrown().toString();
		if (co.caughtExceptionObj instanceof UncaughtUserException)
		    return ((UncaughtUserException) co.caughtExceptionObj).getUncaught().toString();
		return co.caughtExceptionObj.toString();
	    }
	}));

	inst.defineJFunc("Coroutine_GetBResult", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalReturnTyped<>(
		Boolean.class)));
	inst.defineJFunc("Coroutine_GetIResult", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalReturnTyped<>(
		Integer.class)));
	inst.defineJFunc("Coroutine_GetRResult", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalReturnTyped<>(
		Double.class)));
	inst.defineJFunc("Coroutine_GetSResult", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalReturnTyped<>(
		String.class)));
	inst.defineJFunc("Coroutine_GetHResult", BaseVMJFunctionImpl.generateFunctionLambda(new CoroutineFuncInternalReturnTyped<>(
		Object.class)));
    }

}
