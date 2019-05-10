package logicbyte.vm.exception;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMInvokable;

/**
 *
 * @author Lochie
 */
public class NoSuchFunctionException extends VMException {

    public NoSuchFunctionException(LogicVM vm, VMInvokable invoker, String message) {
	super(vm, invoker, message, null);
    }
}
