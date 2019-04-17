package logicbyte.bytecode.validate;

/**
 *
 * @author Lochie
 */
public class LinkerValidationException extends Exception {

    public LinkerValidationException(String message) {
	super(message);
    }

    public LinkerValidationException(String message, Throwable inner) {
	super(message, inner);
    }

}
