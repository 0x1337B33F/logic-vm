package logicbyte.vm;

import logicbyte.vm.exception.VMException;

/**
 * <p>
 * Contract interface for classes which implement virtual-machine invokable functions.
 * </p>
 *
 * @author Lochie
 */
public interface VMInvokable {

    /**
     * Set the debugger instance for the invokable. The debugger shall be used for subsequent operation(s) on this invokable.
     *
     * @param callback The callback or null
     */
    public void debugger(VMDebugCallback callback);

    /**
     * Begin a new invocation of the invokable function with the debugger provided and the arguments array specified. The arguments, if not
     * null, will be placed on the stack of the virtual machine for the invocation of the function.
     *
     * @param callback  The callback or null
     * @param arguments The arguments to the call or null
     *
     * @throws VMException
     */
    public void invoke(VMDebugCallback callback, Object[] arguments) throws VMException;

    /**
     * Called to advance execution of the invokable reference.
     *
     * @param callback The callback or null
     *
     * @return If the invocation has completed or not
     *
     * @throws InterruptedException If an interrupt is raised during execution, the interrupt will not be absorbed.
     * @throws VMException
     */
    public boolean advance(VMDebugCallback callback) throws InterruptedException, VMException;

    /**
     * Called by native functions only to invoke function pointers on the current stack.
     *
     * @param callback     The callback or null
     * @param tailcallFunc The function pointer to invoke
     * @param arguments    The arguments to pass to the function invocation
     * @param wantsReturn  If the callee desires the return value, if any
     *
     * @return The return from the function, or null if no return value
     *
     * @throws InterruptedException If an interrupt is raised during execution, the interrupt will not be absorbed.
     * @throws VMException
     */
    public Object tailcall(VMDebugCallback callback, VMFunctionPointer tailcallFunc, Object[] arguments, boolean wantsReturn) throws InterruptedException, VMException;

    /**
     * @return The debugging access object for this invokable instance.
     */
    public VMInvokableAccess access();

}
