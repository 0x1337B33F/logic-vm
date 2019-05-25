package logicbyte.vm;

import logicbyte.vm.exception.NoSuchTypeException;

/**
 * <p>
 * Contract interface for classes which implement virtual machine function pointers.
 * </p>
 *
 * @author Lochie
 */
public interface VMFunctionPointer {

    /**
     * De-reference the function pointer to the underlying function; if the function is no longer loaded, null is returned.
     *
     * @return The function implementation object, or null if the function is no longer loaded.
     */
    public VMFunction dereference();

    /**
     * Get the name of the function, or null if the function is no longer loaded.
     *
     * @return The function mame, or null if the function is no longer loaded.
     */
    public String name();

    /**
     * Get the return type-name of the function, or null if the function no longer loaded.
     *
     * @return The function return type-name, or null if the function is no longer loaded.
     */
    public VMType returnTypeOfFunction() throws NoSuchTypeException;

    /**
     * Get the argument(s) to the function, or an empty array if no arguments, or null if the function is no longer loaded.
     *
     * @return The function argument type-names, or null if the function is no longer loaded.
     */
    public VMType[] argumentTypesOfFunction() throws NoSuchTypeException;

}
