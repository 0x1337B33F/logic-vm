package logicbyte.vm;

import java.lang.reflect.InvocationTargetException;
import logictype.ThrowObjectException;

/**
 * <p>
 * Contract interface for classes which implement function wrappers which can be used to invoke a Java instance method, static method or
 * anonymous lambda function from a native function call within the VM instance.</p>
 *
 * @author Lochie
 */
public interface VMJFunction {

    /**
     * Called from the VM to invoke the VMJFunction.
     *
     * @param vm        The virtual machine instance whom the executor belongs to
     * @param executor  The executor instance which is executing bytecode and the current function call
     * @param debug     The debugger of the call, or the debugger of the executor, or <code>null</code> if neither is assigned
     * @param arguments The arguments provided to the native function call, removed from the VM stack
     *
     * @return Any value which shall be placed on the VM stack, or <code>null</code> if no return result is required
     *
     * @throws IllegalAccessException    If the current Class does not have access to the specified Class or Method, an
     *                                   {@link IllegalAccessException} will be thrown.
     * @throws IllegalArgumentException  If the invocation arguments do not match the Method signature, a {@link IllegalArgumentException}
     *                                   will be thrown.
     * @throws InvocationTargetException If the target method is static and an instance is provided, or, the method is instance and an
     *                                   instance is not provided, a {@link InvocationTargetException} will be thrown.
     * @throws ThrowObjectException	     Thrown from user code to raise an exception into the machine that shall not be of the string type.
     */
    public Object invoke(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] arguments) throws
	    IllegalAccessException, IllegalArgumentException, InvocationTargetException, ThrowObjectException;

}
