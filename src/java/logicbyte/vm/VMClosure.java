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

    public void load( final VMDebugCallback callback ) throws InterruptedException, VMException;

    public VMGlobalsTable globals();

    public VMClosureTypeMapper types();

    public VMFunction getFunction(String name);

    public LogicVM getExecutor();

    public LinkerObjectFile getLinker();

    public LogicBytecodeFile getBytecode();

}
