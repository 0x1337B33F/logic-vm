package logicbyte.vm.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.VMClosureTypeMapper;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMGlobalsTable;
import logicbyte.vm.VMGlobalsTable.GlobalType;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMType;
import logicbyte.vm.exception.UnsatisfiedImportException;
import logicbyte.vm.exception.VMException;

/**
 *
 * @author Lochie
 */
public class BaseVMClosureImpl implements VMClosure {

    final LogicVM executorObj;
    final LinkerObjectFile linkerObj;
    final LogicBytecodeFile bytecodeObj;

    final BaseVMClosureTypeMapper typesTable;
    final BaseVMGlobalsTable globalsTable;

    final Map<String, VMFunction> functionMap;

    public BaseVMClosureImpl(LogicVM executor, LinkerObjectFile linker, LogicBytecodeFile bytecode) {
	this.executorObj = executor;
	this.linkerObj = linker;
	this.bytecodeObj = bytecode;
	this.typesTable = new BaseVMClosureTypeMapper();
	this.globalsTable = new BaseVMGlobalsTable();
	this.functionMap = new HashMap<>();
    }

    @Override
    public void load(final VMDebugCallback callback) throws InterruptedException, VMException {

	if (linkerObj.importsTable != null && !linkerObj.importsTable.isEmpty()) {
	    LinkedList<LinkerObjectFile> linkers = new LinkedList<>();
	    executorObj.loaded(linkers);

	    Iterator<ImportsEntry> itx0 = linkerObj.importsTable.iterator();
	    while (itx0.hasNext()) {
		ImportsEntry importsDecl = itx0.next();

		if ((importsDecl.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_TYPE) != 0) {
		    Iterator<String> itx1 = importsDecl.labels.iterator();
		    Iterator<Integer> itx2 = importsDecl.slots.iterator();

		    while (itx2.hasNext()) {
			final String label = itx1.next();
			final int slot = itx2.next();
			LinkerObjectFile match = null;

			Iterator<LinkerObjectFile> itx3 = linkers.iterator();
			l0:
			while (itx3.hasNext()) {
			    LinkerObjectFile linker = itx3.next();
			    if (linker.exportsTable == null || linker.exportsTable.isEmpty())
				continue;
			    Iterator<ExportsEntry> itx4 = linker.exportsTable.iterator();
			    while (itx4.hasNext()) {
				ExportsEntry e = itx4.next();
				if (!e.name.equals(importsDecl.name))
				    continue;
				if ((e.typeflags & LinkerObjectFile.TYPEFLAGS_EXPORTS_TYPE) != 0 && e.labels.contains(label)) {
				    match = linker;
				    break l0;
				}
			    }
			}

			if (match == null)
			    throw new UnsatisfiedImportException(executorObj, String.format(
				    "expected that type-symbol `%s` (from `%s`) is loaded",
				    label, importsDecl.name));

			VMType typeMatch = executorObj.getType(executorObj.getClosure(match), label);
			if (typeMatch == null)
			    throw new UnsatisfiedImportException(executorObj, String.format(
				    "expected that type-symbol `%s` (from `%s`) is defined in closure `%s`",
				    label, importsDecl.name, match.source()));

			typesTable.defineType(typeMatch, slot, false);
		    }
		}
	    }
	}

	Iterator<TypesEntry> itx2 = linkerObj.typesTable.iterator();
	while (itx2.hasNext()) {
	    TypesEntry typeDecl = itx2.next();

	    if ((typeDecl.typeflags & LinkerObjectFile.TYPEFLAGS_TYPES_USER) != 0) {
		VMType superType = typesTable.getTypeOf(typeDecl.superName);
		if (superType == null)
		    throw new UnsatisfiedImportException(executorObj, String.format(
			    "expected that type-symbol `%s` is loaded for type-declaration `%s`",
			    typeDecl.superName, typeDecl.name));
		typesTable.defineType(executorObj.defineType(this, typeDecl.name, superType), typeDecl.slot, true);
	    } else
		typesTable.defineType(executorObj.defineType(this, typeDecl.name), typeDecl.slot, true);
	}

	Iterator<GlobalsEntry> itx3 = linkerObj.globalsTable.iterator();
	while (itx3.hasNext()) {
	    GlobalsEntry globalDecl = itx3.next();
	    VMType typeOfGlobal = typesTable.getTypeOf(globalDecl.type);
	    if ((globalDecl.globalsflags & LinkerObjectFile.TYPEFLAGS_GLOBAL_ISNATIVE) != 0)
		globalsTable.defineGlobal(globalDecl.name, GlobalType.NATIVEGLOBAL, typeOfGlobal,
			(globalDecl.globalsflags & LinkerObjectFile.TYPEFLAGS_GLOBAL_ISARRAY) != 0, null);
	    else
		globalsTable.defineGlobal(globalDecl.name, GlobalType.USERGLOBAL, typeOfGlobal,
			(globalDecl.globalsflags & LinkerObjectFile.TYPEFLAGS_GLOBAL_ISARRAY) != 0, null);
	}

	BytecodeFunctionDecl[] funcs = bytecodeObj.getAll();
	for (BytecodeFunctionDecl func : funcs) {
	    BaseVMFunctionImpl allocFuncImpl = new BaseVMFunctionImpl(this, func);
	    functionMap.put(func.name(), allocFuncImpl);
	}

	if (functionMap.containsKey("<ginit>")) {
	    VMInvokable callable = this.executorObj.invoke(callback, functionMap.get("<ginit>"));
	    callable.invoke(callback, null);
	    while (!callable.advance(callback));
	}
    }

    @Override
    public VMGlobalsTable globals() {
	return globalsTable;
    }

    @Override
    public VMClosureTypeMapper types() {
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
