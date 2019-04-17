package logicbyte.vm.runtime.factory;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.factory.VMClosureFactory;
import logicbyte.vm.runtime.BaseVMClosureImpl;

/**
 *
 * @author Lochie
 */
public class BaseVMClosureFactory implements VMClosureFactory {

    @Override
    public VMClosure create(LogicVM vm, LinkerObjectFile linker, LogicBytecodeFile bytecode) throws Exception {
	return new BaseVMClosureImpl(vm, linker, bytecode);
    }

}
