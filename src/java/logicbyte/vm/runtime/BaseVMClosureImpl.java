package logicbyte.vm.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMGlobalsTable;
import logicbyte.vm.VMGlobalsTable.GlobalType;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMTypesTable;
import logicbyte.vm.exception.VMException;

/**
 *
 * @author Lochie
 */
public class BaseVMClosureImpl implements VMClosure {

    final LogicVM executorObj;
    final LinkerObjectFile linkerObj;
    final LogicBytecodeFile bytecodeObj;

    final BaseVMTypesTable typesTable;
    final BaseVMGlobalsTable globalsTable;

    final Map<String, VMFunction> functionMap;

    public BaseVMClosureImpl(LogicVM executor, LinkerObjectFile linker, LogicBytecodeFile bytecode) {
	this.executorObj = executor;
	this.linkerObj = linker;
	this.bytecodeObj = bytecode;
	this.typesTable = new BaseVMTypesTable();
	this.globalsTable = new BaseVMGlobalsTable();
	this.functionMap = new HashMap<>();
    }

    @Override
    public void load() throws InterruptedException, VMException {
	Iterator<TypesEntry> itx0 = linkerObj.typesTable.iterator();
	while (itx0.hasNext()) {
	    TypesEntry typeDecl = itx0.next();
	    typesTable.defineType(typeDecl.slot, typeDecl);
	}

	Iterator<GlobalsEntry> itx1 = linkerObj.globalsTable.iterator();
	while (itx1.hasNext()) {
	    GlobalsEntry globalDecl = itx1.next();
	    if ((globalDecl.globalsflags & LinkerObjectFile.TYPEFLAGS_GLOBAL_ISNATIVE) != 0)
		globalsTable.defineGlobal(globalDecl.name, GlobalType.NATIVEGLOBAL, globalDecl.type, null);
	    else
		globalsTable.defineGlobal(globalDecl.name, GlobalType.USERGLOBAL, globalDecl.type, null);
	}

	BytecodeFunctionDecl[] funcs = bytecodeObj.getAll();
	for (BytecodeFunctionDecl func : funcs) {
	    BaseVMFunctionImpl allocFuncImpl = new BaseVMFunctionImpl(this, func);
	    functionMap.put(func.name(), allocFuncImpl);
	}

	if (functionMap.containsKey("<ginit>")) {
	    VMInvokable callable = this.executorObj.invoke(null, functionMap.get("<ginit>"));
	    callable.invoke(null, null);
	    while (!callable.advance(null));
	}
    }

    @Override
    public VMGlobalsTable globals() {
	return globalsTable;
    }

    @Override
    public VMTypesTable types() {
	return typesTable;
    }

    @Override
    public VMFunction getFunction(String name) {
	return functionMap.get(name);
    }

    @Override
    public LogicVM getExecutor() {
	return executorObj;
    }

    @Override
    public LinkerObjectFile getLinker() {
	return linkerObj;
    }

    @Override
    public LogicBytecodeFile getBytecode() {
	return bytecodeObj;
    }

    @Override
    public boolean equals(Object obj) {
	if (!(obj instanceof VMClosure))
	    return false;
	VMClosure other = (VMClosure) obj;
	return this.executorObj.equals(other.getExecutor())
		&& this.linkerObj.equals(other.getLinker())
		&& this.bytecodeObj.equals(other.getBytecode());
    }

}
