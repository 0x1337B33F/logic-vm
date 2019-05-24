package logicbyte.vm.exception;

/**
 *
 * @author Lochie
 */
public class DuplicateTypeNameException extends VMException {

    final String nameOfType;

    public DuplicateTypeNameException(String typeName) {
	super(null);
	this.nameOfType = typeName;
    }

    public DuplicateTypeNameException(String typeName, String message) {
	super(message);
	this.nameOfType = typeName;
    }

    public String getTypeName() {
	return this.nameOfType;
    }

}
