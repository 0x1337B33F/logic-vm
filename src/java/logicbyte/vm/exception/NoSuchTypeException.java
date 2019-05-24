package logicbyte.vm.exception;

/**
 * <p>
 * Thrown when a reference is made to a type in the closure-mapper when the type-name or type-number specified is unknown.</p>
 *
 * @author Lochie
 */
public class NoSuchTypeException extends VMException {

    final String nameOfType;
    final Integer numberOfType;

    public NoSuchTypeException(String typeName) {
	super(String.format("type-name: %s", typeName));
	this.nameOfType = typeName;
	this.numberOfType = null;
    }

    public NoSuchTypeException(int typeNumber) {
	super(String.format("type-number: %s", typeNumber));
	this.nameOfType = null;
	this.numberOfType = typeNumber;
    }

    public String getTypeName() {
	return this.nameOfType;
    }

    public Integer getTypeNumber() {
	return this.numberOfType;
    }

}
