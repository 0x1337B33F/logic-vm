package logicbyte.vm.exception;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public class IllegalTypeMutationException extends VMException {

    private final VMType targetType;

    public IllegalTypeMutationException(LogicVM vm, String message) {
	this(vm, null, message);
    }

    public IllegalTypeMutationException(LogicVM vm, VMType type, String message) {
	super(vm, message);
	this.targetType = type;
    }

    public VMType getTargetType() {
	return this.targetType;
    }

}
