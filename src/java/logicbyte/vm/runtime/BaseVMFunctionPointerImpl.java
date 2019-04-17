package logicbyte.vm.runtime;

import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMFunctionPointer;
import logicbyte.vm.VMTypesTable;

/**
 *
 * @author Lochie
 */
public class BaseVMFunctionPointerImpl implements VMFunctionPointer {

    protected final VMFunction funcImpl;

    public BaseVMFunctionPointerImpl(VMFunction func) {
	this.funcImpl = func;
    }

    @Override
    public VMFunction dereference() {
	return funcImpl;
    }

    @Override
    public String name() {
	return funcImpl.name();
    }

    @Override
    public String returnTypeOfFunction() {
	BytecodeFunctionDecl bytecode = this.funcImpl.bytecode();
	return (bytecode.hasreturn()) ? this.funcImpl.closure().types().getTypeOf(bytecode.returntype()).name : "nothing";
    }

    @Override
    public String[] argumentTypesOfFunction() {
	if (!this.funcImpl.bytecode().hasargs())
	    return new String[ 0 ];
	BytecodeFunctionDecl bytecode = this.funcImpl.bytecode();
	VMTypesTable typesTable = this.funcImpl.closure().types();
	String[] typenames = new String[ bytecode.numargs() ];
	for (int i = 0, j = typenames.length; i < j; i++)
	    typenames[i] = typesTable.getTypeOf(bytecode.argtype(i)).name;
	return typenames;
    }

    @Override
    public boolean equals(Object obj) {
	if (!(obj instanceof VMFunctionPointer))
	    return false;
	VMFunction function = ((VMFunctionPointer) obj).dereference();
	if (function == null)
	    return false;
	return this.funcImpl.equals(function);
    }

}
