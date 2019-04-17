package logicbyte.bytecode.validate;

/**
 *
 * @author Lochie
 */
public class BytecodeValidationException extends Exception {

    public BytecodeValidationException(String message) {
	super(message);
    }

    public BytecodeValidationException(String message, Throwable inner) {
	super(message, inner);
    }
}
