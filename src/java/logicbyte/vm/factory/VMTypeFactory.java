package logicbyte.vm.factory;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public interface VMTypeFactory {

    public VMType create(LogicVM vm, LinkerObjectFile linker, LogicBytecodeFile bytecode, VMClosure closure, String nameOfType) throws Exception;

    public VMType create(LogicVM vm, LinkerObjectFile linker, LogicBytecodeFile bytecode, VMClosure closure, String nameOfType,
	    VMType superType) throws Exception;

}
