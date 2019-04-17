package logicbyte.vm.exception;

import logicbyte.vm.LogicVM;

/**
 *
 * @author Lochie
 */
public class UnsatisfiedImportException extends VMException {

    public UnsatisfiedImportException(LogicVM vm, String message) {
	super(vm, message);
    }

    public UnsatisfiedImportException(LogicVM vm, String message, Throwable inner) {
	super(vm, message, inner);
    }
}
