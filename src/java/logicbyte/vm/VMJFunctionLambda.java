package logicbyte.vm;

import logicbyte.vm.exception.ThrowObjectException;

/**
 * Interface for anonymous function bodies.
 */
public interface VMJFunctionLambda {

    /**
     * Invoked by the VM when the native function is invoked from bytecode.
     *
     * @param vm        The virtual machine instance.
     * @param executor  The executor instance.
     * @param debug     The debugger for the call, or the executor, or <code>null</code> if none is assigned.
     * @param arguments The parameters provided to the native function call, removed from the VM stack.
     *
     * @return Any value which shall be placed on the VM stack after execution, or <code>null</code> if no return value.
     *
     * @throws logicbyte.vm.exception.ThrowObjectException An object exception box may be thrown, which may be caught type-wise by the bytecode or
     *                                        shall be passed to the invoker of the executor.
     *
     * @throws Exception                      Any exception may be thrown, which may be caught by bytecode or shall be passed to the
     *                                        invoker of the executor.
     */
    public Object call(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] arguments) throws ThrowObjectException, Exception;
    
}
