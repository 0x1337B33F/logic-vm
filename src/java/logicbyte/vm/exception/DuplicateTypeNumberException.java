package logicbyte.vm.exception;

/**
 *
 * @author Lochie
 */
public class DuplicateTypeNumberException extends VMException {

    final int numberOfType;

    public DuplicateTypeNumberException(String message) {
	super(message);
	this.numberOfType = -1;
    }

    public DuplicateTypeNumberException(int typeNumber) {
	super(null);
	this.numberOfType = typeNumber;
    }

    public DuplicateTypeNumberException(String message, int typeNumber) {
	super(message);
	this.numberOfType = typeNumber;
    }

    public int getTypeNumber() {
	return this.numberOfType;
    }

}
