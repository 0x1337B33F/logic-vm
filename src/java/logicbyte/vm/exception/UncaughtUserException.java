package logicbyte.vm.exception;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;

/**
 *
 * @author Lochie
 */
public class UncaughtUserException extends VMException {

    final Object uncaught;

    public UncaughtUserException(LogicVM vm, VMClosure closure, Object uncaught) {
	super(vm, closure, null, null);
	this.uncaught = uncaught;
    }

    public Object getUncaught() {
	return this.uncaught;
    }
}
