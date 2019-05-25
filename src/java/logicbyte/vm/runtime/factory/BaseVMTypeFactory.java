package logicbyte.vm.runtime.factory;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.factory.VMTypeFactory;
import logicbyte.vm.runtime.BaseVMUserType;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public class BaseVMTypeFactory implements VMTypeFactory {

    @Override
    public VMType create(LogicVM vm, LinkerObjectFile linker, LogicBytecodeFile bytecode, VMClosure closure, String nameOfType) throws Exception {
	return create(vm, linker, bytecode, closure, nameOfType, null);
    }

    @Override
    public VMType create(LogicVM vm, LinkerObjectFile linker, LogicBytecodeFile bytecode, VMClosure closure, String nameOfType,
	    VMType superType) throws Exception {
	return new BaseVMUserType(closure, nameOfType, superType);
    }

}
