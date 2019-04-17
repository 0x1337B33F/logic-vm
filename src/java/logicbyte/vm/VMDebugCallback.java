package logicbyte.vm;

import logicbyte.bytecode.Opcode;

/**
 * <p>
 * Contract interface for classes which implement virtual machine debugging callback handlers.</p>
 *
 * @author Lochie
 */
public interface VMDebugCallback {

    /**
     * Called when a new function is entered.
     *
     * @param executor     The invoke instance
     * @param caller       The caller function, or null for the entry function
     * @param callee       The callee function
     * @param generatingOp The opcode which generated the function entry, or null for the entry function
     */
    public void enterfunc(VMInvokable executor, VMFunction caller, VMFunction callee, Opcode generatingOp);

    /**
     * Called when a function is exited.
     *
     * @param executor    The invoke instance
     * @param caller      The caller function, or null for the entry function
     * @param callee      The callee function
     * @param returnValue The return value from the callee's return
     */
    public void exitfunc(VMInvokable executor, VMFunction caller, VMFunction callee, Object returnValue);

    /**
     * Called when a function is removed from the call stack, such as when unwinding the stack to handle an uncaught exception.
     *
     * @param executor   The invoke instance
     * @param caller     The caller function, or null for the entry function
     * @param callee     The callee function
     * @param raiseValue The value being raised
     */
    public void unwindfunc(VMInvokable executor, VMFunction caller, VMFunction callee, Object raiseValue);

    /**
     * Called before an operand is evaluated.
     *
     * @param executor The invoke instance
     * @param func     The function declaration
     * @param op       The operand
     */
    public void beforeop(VMInvokable executor, VMFunction func, Opcode op);

    /**
     * Called when a BREAK opcode is hit.
     *
     * @param executor   The invoke instance
     * @param func       The function declaration
     * @param breakpoint The breakpoint opcode
     */
    public void breakpoint(VMInvokable executor, VMFunction func, Opcode breakpoint);

    /**
     * Called after an operand is evaluated.
     *
     * @param executor The invoke instance
     * @param func     The function declaration
     * @param op       The operand
     */
    public void afterop(VMInvokable executor, VMFunction func, Opcode op);

}
