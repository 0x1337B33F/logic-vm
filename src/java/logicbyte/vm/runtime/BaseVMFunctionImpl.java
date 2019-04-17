package logicbyte.vm.runtime;

import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.vm.VMClosure;
import logicbyte.vm.VMFunction;

/**
 *
 * @author Lochie
 */
public class BaseVMFunctionImpl implements VMFunction {

    final BaseVMClosureImpl closureRef;

    final BytecodeFunctionDecl funcBytecodeObj;

    public BaseVMFunctionImpl(BaseVMClosureImpl closure, BytecodeFunctionDecl funcBytecodeObj) {
	this.closureRef = closure;
	this.funcBytecodeObj = funcBytecodeObj;
    }

    @Override
    public String name() {
	return funcBytecodeObj.name();
    }

    @Override
    public VMClosure closure() {
	return closureRef;
    }

    @Override
    public BytecodeFunctionDecl bytecode() {
	return funcBytecodeObj;
    }

    @Override
    public boolean equals(Object obj) {
	if (!(obj instanceof VMFunction))
	    return false;
	VMFunction other = (VMFunction) obj;

	return this.closureRef.equals(other.closure())
		&& this.funcBytecodeObj.equals(other.bytecode());
    }

}
