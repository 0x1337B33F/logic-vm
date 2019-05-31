package logicbyte.vm;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.vm.exception.VMException;

/**
 * <p>
 * Contract interface for classes which implement virtual-machine file-level closures; the file closure contains the set of:</p>
 * <ul>
 * <li>The loaded function set, which contains all the functions which have been defined in the bytecode;</li>
 * <li>The loaded globals table, which contains all defined file-local globals and their associated value box; and</li>
 * <li>The loaded types table, which contains the type declaration-to-type number mappings.</li>
 * </ul>
 *
 * @author Lochie
 */
public interface VMClosure {

    /**
     * <p>
     * Called to initialize the closure.</p>
     *
     * @param callback The debugging callback
     *
     * @throws InterruptedException If an interrupt is raised, it will not be absorbed
     * @throws VMException          If the initialization function raises an error, a {@link VMException} will be thrown
     */
    public void load(final VMDebugCallback callback) throws InterruptedException, VMException;

    /**
     * @return The globals table for the closure
     */
    public VMGlobalsTable globals();

    /**
     * @return The type-mapper for the closure
     */
    public VMClosureTypeMapper types();

    /**
     * <p>
     * Get a function declaration by function name. If the function does not exist, returns <code>null</code>.</p>
     *
     * @param name The name of the function declaration
     *
     * @return The function declaration, or <code>null</code> if no such function exists
     */
    public VMFunction getFunction(String name);

    /**
     * @return The VM instance to which the closure belongs
     */
    public LogicVM getExecutor();

    /**
     * @return The linker definition which generated the closure
     */
    public LinkerObjectFile getLinker();

    /**
     * @return The bytecode definition which generated the closure
     */
    public LogicBytecodeFile getBytecode();

}
