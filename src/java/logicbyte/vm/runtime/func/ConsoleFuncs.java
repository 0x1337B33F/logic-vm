package logicbyte.vm.runtime.func;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.runtime.BaseVMJFunctionImpl.VMJFunctionLabel;

/**
 *
 * @author Lochie
 */
public class ConsoleFuncs {

    @VMJFunctionLabel(name = "PrintConsole")
    public static void printStandardOutput(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	System.out.println(args[0]);
    }

    @VMJFunctionLabel(name = "PrintError")
    public static void printErrorOutput(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	System.err.println(args[0]);
    }

}
