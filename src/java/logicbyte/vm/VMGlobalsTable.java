package logicbyte.vm;

/**
 * <p>
 * Contract interface for classes which implement globals table collections.
 * </p>
 *
 * @author Lochie
 */
public interface VMGlobalsTable {

    /**
     * Enumeration which defines the type of the global.
     */
    public static enum GlobalType {
	/**
	 * The global is a Java-defined global.
	 */
	NATIVEGLOBAL,
	/**
	 * The global was defined from user code.
	 */
	USERGLOBAL;
    }

    /**
     * The value box used to contain the value of the global for cross-Closure access.
     */
    public static class GlobalBox {

	/**
	 * The type of the global.
	 */
	public GlobalType typeOfGlobal;

	/**
	 * The type of the global value.
	 */
	public int typeOfGlobalValue;

	/**
	 * The current Object which is the value of the global, or null if the global is unset.
	 */
	public Object valueInGlobalBox;

    }

    /**
     * Define a new global in the assigned Closure.
     *
     * @param nameOfGlobalValue        The name of the global variable
     * @param typeOfGlobal             The type of the global
     * @param typeOfGlobalValue        The type of the global value
     * @param initialGlobalValueObject The initial value for the global, or null
     *
     * @return If the global was defined or not
     */
    public boolean defineGlobal(String nameOfGlobalValue, GlobalType typeOfGlobal, int typeOfGlobalValue, Object initialGlobalValueObject);

    /**
     * Get the value-box for the global specified.
     *
     * @param nameOfGlobalValue The name of the global variable
     *
     * @return The global value box, or null if the global does not exist
     */
    public GlobalBox getGlobal(String nameOfGlobalValue);

}
