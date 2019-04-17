package logicbyte.vm;

import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;

/**
 * <p>
 * Contract interface for classes which implement virtual machine internal functions, such as those loaded from bytecode declarations.
 * </p>
 *
 * @author Lochie
 */
public interface VMFunction {

    /**
     * Get the name of the function.
     *
     * @return The name of the function.
     */
    public String name();

    /**
     * Get the associated Closure which contains the function.
     *
     * @return The associated Closure.
     */
    public VMClosure closure();

    /**
     * Get the bytecode declaration object which contains the bytecode for the function.
     *
     * @return The bytecode object which contains the bytecode for the function.
     */
    public BytecodeFunctionDecl bytecode();

}
