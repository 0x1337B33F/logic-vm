package logicbyte.vm.factory;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;

/**
 * <p>
 * Factory for VM instance closure mutation(s).</p>
 *
 * @author Lochie
 */
public interface VMClosureFactory {

    public VMClosure create(LogicVM vm, LinkerObjectFile linker, LogicBytecodeFile bytecode) throws Exception;

}
