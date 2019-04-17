package logicbyte.vm.exception;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;

/**
 *
 * @author Lochie
 */
public class VMException extends Exception {

    protected LogicVM vmInstanceObj;
    protected VMClosure vmFileClosureObj;

    public VMException(String message) {
	this(null, null, message, null);
    }

    public VMException(String message, Throwable inner) {
	this(null, null, message, inner);
    }

    public VMException(LogicVM vm, String message) {
	this(vm, null, message, null);
    }

    public VMException(LogicVM vm, String message, Throwable inner) {
	this(vm, null, message, inner);
    }

    public VMException(LogicVM vm, VMClosure closure, String message, Throwable inner) {
	super(message, inner);
	this.vmInstanceObj = vm;
	this.vmFileClosureObj = closure;
    }

}
