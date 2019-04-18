package logicbyte.vm;

/**
 * <p>
 * Provides non-reflective access to virtual machine invokable internals.</p>
 *
 * @author Lochie
 */
public interface VMInvokableAccess {

    /**
     * @return The function declaration which was used to instantiate the invokable
     */
    public VMFunction getEntryFunction();

    /**
     * @return Get the size of the function call stack
     */
    public int getCallStackSize();

    /**
     * @param stackSlot The function call stack index
     *
     * @return The function declaration in the call stack
     */
    public VMFunction getFunctionOnCallStack(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot The function call stack index
     *
     * @return The function program counter value
     */
    public int getFunctionProgramCounter(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot The function call stack index
     *
     * @return The function comparison register value
     */
    public boolean getFunctionComparisonRegister(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot The function call stack index
     *
     * @return The caught exception object value
     */
    public Object getFunctionCaughtExceptionObj(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot The function call stack index
     *
     * @return The function desires call result flag value
     */
    public boolean getFunctionDesiresCallResult(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot The function call stack index
     *
     * @return The number of closures for the function call
     */
    public int getFunctionClosureCount(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot   The function call stack index
     * @param closureSlot The function closure stack index
     *
     * @return The start index of the closure locals
     */
    public int getFunctionClosureLocalsStart(int stackSlot, int closureSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot   The function call stack index
     * @param closureSlot The function closure stack index
     *
     * @return The number of closure locals
     */
    public int getFunctionClosureLocalsCount(int stackSlot, int closureSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot   The function call stack index
     * @param closureSlot The function closure stack index
     * @param localNumber The local number
     *
     * @return The type number of the local
     */
    public int getFunctionClosureLocalsType(int stackSlot, int closureSlot, int localNumber) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot   The function call stack index
     * @param closureSlot The function closure stack index
     * @param localNumber The local number
     *
     * @return If the closure local value is an array
     */
    public boolean getFunctionClosureLocalsArray(int stackSlot, int closureSlot, int localNumber) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot   The function call stack index
     * @param closureSlot The function closure stack index
     * @param localNumber The local number
     *
     * @return The size of the array
     */
    public int getFunctionClosureLocalsArraySize(int stackSlot, int closureSlot, int localNumber) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot   The function call stack index
     * @param closureSlot The function closure stack index
     * @param localNumber The local number
     *
     * @return The closure local value object
     */
    public Object getFunctionClosureLocalsValue(int stackSlot, int closureSlot, int localNumber) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot   The function call stack index
     * @param closureSlot The function closure stack index
     * @param localNumber The local number
     * @param arraySlot   The array index
     *
     * @return The closure local array value object
     */
    public Object getFunctionClosureLocalsArrayValue(int stackSlot, int closureSlot, int localNumber, int arraySlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot The function call stack index
     *
     * @return The size of the function stack
     */
    public int getFunctionStackCount(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot     The function call stack index
     * @param funcStackSlot The function stack index
     *
     * @return
     */
    public Object getFunctionStackValue(int stackSlot, int funcStackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot The function call stack index
     *
     * @return The size of the try-catch stack
     */
    public int getFunctionTryCatchStackCount(int stackSlot) throws IndexOutOfBoundsException;

    /**
     * @param stackSlot         The function call stack index
     * @param tryCatchStackSlot The try-catch stack index
     *
     * @return The try-catch jump address
     */
    public int getFunctionTryCatchStackJumpDest(int stackSlot, int tryCatchStackSlot) throws IndexOutOfBoundsException;

    public int getFunctionTryCatchStackClosureDepth(int stackSlot, int tryCatchStackSlot) throws IndexOutOfBoundsException;

}
