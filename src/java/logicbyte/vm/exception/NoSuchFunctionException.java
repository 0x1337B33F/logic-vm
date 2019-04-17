package logicbyte.vm.exception;

import logicbyte.vm.LogicVM;

/**
 *
 * @author Lochie
 */
public class NoSuchFunctionException extends VMException {

    public NoSuchFunctionException(LogicVM vm, String message) {
	super(vm, message, null);
    }
}
