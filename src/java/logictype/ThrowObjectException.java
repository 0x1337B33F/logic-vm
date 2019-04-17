package logictype;

/**
 * <p>
 * Value box that allows any value to be thrown, as oppose to that which implements the Throwable interface.</p>
 *
 * @author Lochie
 */
public class ThrowObjectException extends Exception {

    final Object thrownObj;

    public ThrowObjectException(Object thrownObj) {
	this.thrownObj = thrownObj;
    }

    public Object getThrown() {
	return this.thrownObj;
    }
}
