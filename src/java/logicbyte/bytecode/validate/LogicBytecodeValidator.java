package logicbyte.bytecode.validate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import logicbyte.bytecode.FunctionOpcodeList;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.Opcodes;
import logicbyte.bytecode.validate.BytecodeValidationException.IllegalOperandException;
import logictype.Pair;

/**
 *
 * @author Lochie
 */
public class LogicBytecodeValidator {

    /**
     * The lookup map of type-names to type import declarations
     */
    private final Map<Integer, ImportsEntry> mapTypesImports = new HashMap<>();

    /**
     * The lookup map of type-names to type declarations
     */
    private final Map<Integer, TypesEntry> mapTypesDecls = new HashMap<>();

    /**
     * The lookup map of global-names to global import declarations
     */
    private final Map<String, ImportsEntry> mapGlobalsImports = new HashMap<>();
    /**
     * The lookup map of global-names to global declarations
     */
    private final Map<String, GlobalsEntry> mapGlobalsDecls = new HashMap<>();

    public void reset() {
	mapTypesImports.clear();
	mapTypesDecls.clear();
	mapGlobalsImports.clear();
	mapGlobalsDecls.clear();
    }

    public void validate(LinkerObjectFile linker, LogicBytecodeFile bytecode) throws LinkerValidationException, BytecodeValidationException {
	generateTypesTables(linker, bytecode);
	generateGlobalsTables(linker, bytecode);
	validateFunctions(linker, bytecode);
    }

    private void generateTypesTables(LinkerObjectFile linker, LogicBytecodeFile bytecode) {
	if (linker.importsTable != null && !linker.importsTable.isEmpty()) {
	    Iterator<ImportsEntry> itx = linker.importsTable.iterator();
	    while (itx.hasNext()) {
		ImportsEntry entry = itx.next();
		if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_TYPE) != 0) {
		    Iterator<Integer> itx1 = entry.slots.iterator();
		    while (itx1.hasNext())
			this.mapTypesImports.put(itx1.next(), entry);
		}
	    }
	}

	if (linker.typesTable != null && !linker.typesTable.isEmpty()) {
	    Iterator<TypesEntry> itx = linker.typesTable.iterator();
	    while (itx.hasNext()) {
		TypesEntry entry = itx.next();
		this.mapTypesDecls.put(entry.slot, entry);
	    }
	}
    }

    private void generateGlobalsTables(LinkerObjectFile linker, LogicBytecodeFile bytecode) {
	if (linker.importsTable != null && !linker.importsTable.isEmpty()) {
	    Iterator<ImportsEntry> itx = linker.importsTable.iterator();
	    while (itx.hasNext()) {
		ImportsEntry entry = itx.next();
		if ((entry.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL) != 0) {
		    Iterator<String> itx1 = entry.labels.iterator();
		    while (itx1.hasNext())
			this.mapGlobalsImports.put(itx1.next(), entry);
		}
	    }
	}

	if (linker.globalsTable != null && !linker.globalsTable.isEmpty()) {
	    Iterator<GlobalsEntry> itx = linker.globalsTable.iterator();
	    while (itx.hasNext()) {
		GlobalsEntry entry = itx.next();
		this.mapGlobalsDecls.put(entry.name, entry);
	    }
	}
    }

    private void validateFunctions(LinkerObjectFile linker, LogicBytecodeFile bytecode) throws LinkerValidationException, BytecodeValidationException {
	BytecodeFunctionDecl[] funcs = bytecode.getAll();
	for (int i = 0, j = funcs.length; i < j; i++) {
	    final BytecodeFunctionDecl decl = funcs[i];
	    validateFunctionCheckJumps(linker, bytecode, decl);
	    validateFunctionCheckStack(linker, bytecode, decl);
	    validateFunctionCheckLocalUsage(linker, bytecode, decl);
	}
    }

    private void validateFunctionCheckJumps(LinkerObjectFile linker, LogicBytecodeFile bytecode, BytecodeFunctionDecl decl) throws BytecodeValidationException {
	if (decl.isnfunc())
	    return;
	FunctionOpcodeList ops = decl.opcodes();
	for (int i = 0, j = ops.size(); i < j; i++) {
	    Opcode op = ops.get(i),
		    asEnum = op.opcode();
	    if (asEnum.equals(Opcodes.JMP)) {
		if (op.getopt(Opcodes.BITMASK_JMP_JMPS))
		    continue;

		int destAddr = -1;
		if (op.getopt(Opcodes.BITMASK_JMP_AORO))
		    destAddr = (int) op.argobject(0);
		else
		    destAddr = i + (int) op.argobject(0);

		if (destAddr == i)
		    throw new IllegalOperandException(decl, i, op, "JMP: jump to self address");
		if (destAddr < 0 || destAddr >= j)
		    throw new IllegalOperandException(decl, i, op, String.
			    format("JMP: jump beyond opcode limit (to %s, limit %s)", destAddr, j));
	    }
	}

	// TODO: compute jump out of jump loop:
	// OP, OP, OP, OP, JMP EXIT, OP, JMP START (?)
    }

    private void validateFunctionCheckStack(LinkerObjectFile linker, LogicBytecodeFile bytecode, BytecodeFunctionDecl decl) throws BytecodeValidationException {
	if (decl.isnfunc())
	    return;
	FunctionOpcodeList ops = decl.opcodes();
	int stackSize = 0;
	for (int i = 0, j = ops.size(); i < j; i++) {
	    Opcode op = ops.get(i);
	    Opcodes asEnum = op.opcode();

	    switch (asEnum) {
		case NOP:
		case INCLOSURE:
		case ENDCLOSURE:
		case DEBUG:
		case TRY:
		case BREAK:
		    break;

		case JMP:
		    if (op.getopt(Opcodes.BITMASK_JMP_JMPS)) {
			if (stackSize == 0)
			    throw new IllegalOperandException(decl, i, op, "JMP: nothing on stack");
			stackSize--;
		    }
		    break;

		case DUP:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "DUP: nothing on stack");
		    stackSize++;
		    break;

		case PUSHB:
		case PUSHI:
		case PUSHR:
		case PUSHS:
		case FUNCPTR:
		case CATCH:
		    stackSize++;
		    break;

		case POP:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "POP: nothing on stack");
		    stackSize--;
		    break;

		case RETN:
		    if (op.getopt(Opcodes.BITMASK_RETN_RVAL)) {
			if (stackSize == 0)
			    throw new IllegalOperandException(decl, i, op, "RETN: nothing on stack");
			stackSize--;
		    }
		    break;

		case CALL:
		    int args0 = (int) op.argobject(1);

		    if (stackSize < args0)
			throw new IllegalOperandException(decl, i, op, String.format("CALL: require %s values on stack", args0));
		    stackSize -= args0;

		    if (op.getopt(Opcodes.BITMASK_CALL_RVAL))
			stackSize++;
		    break;

		case CALLLOCAL:
		    int args1 = (int) op.argobject(1);

		    if (stackSize < args1)
			throw new IllegalOperandException(decl, i, op, String.format("CALL: require %s values on stack", args1));
		    stackSize -= args1;

		    if (op.getopt(Opcodes.BITMASK_CALLLOCAL_RVAL))
			stackSize++;
		    break;

		case CALLGLOBAL:

		    int args2 = (int) op.argobject(1);

		    if (stackSize < args2)
			throw new IllegalOperandException(decl, i, op, String.format("CALL: require %s values on stack", args2));
		    stackSize -= args2;

		    if (op.getopt(Opcodes.BITMASK_CALLGLOBAL_RVAL))
			stackSize++;
		    break;

		case BINOP:
		    if (stackSize < 2)
			throw new IllegalOperandException(decl, i, op, "BINOP: not enough values on stack");
		    stackSize--;
		    break;

		case UNOP:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "UNOP: nothing on stack");
		    break;

		case NEWUPVAR:
		    if (op.getopt(Opcodes.BITMASK_NEWUPVAR_ARRAY)) {
			if (stackSize == 0)
			    throw new IllegalOperandException(decl, i, op, "NEWUPVAR: no array dimension on stack");
			stackSize--;
		    }
		    break;

		case NEWARRAY:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "NEWARRAY: no array dimension on stack");
		    stackSize--;
		    break;

		case SETLOCAL:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "SETLOCAL: nothing on stack");
		    stackSize--;
		    break;

		case SETGLOBAL:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "SETGLOBAL: nothing on stack");
		    stackSize--;
		    break;

		case SETLOCALARR:
		    if (stackSize < 2)
			throw new IllegalOperandException(decl, i, op, "SETLOCALARR: not enough values on stack");
		    stackSize -= 2;
		    break;

		case SETGLOBALARR:
		    if (stackSize < 2)
			throw new IllegalOperandException(decl, i, op, "SETGLOBALARR: not enough values on stack");
		    stackSize -= 2;
		    break;

		case GETLOCAL:
		case GETGLOBAL:
		    stackSize++;
		    break;

		case GETLOCALARR:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "GETLOCALARR: nothing on stack");
		    break;
		case GETGLOBALARR:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "GETGLOBALARR: nothing on stack");
		    break;

		case YIELD:
		    if (op.getopt(Opcodes.BITMASK_YIELD_STACK)) {
			if (stackSize == 0)
			    throw new IllegalOperandException(decl, i, op, "YIELD: nothing on stack");
			stackSize--;
		    }
		    break;
		case RAISE:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "RAISE: nothing on stack");
		    stackSize--;
		    break;
		case CMP:
		    if (stackSize == 0)
			throw new IllegalOperandException(decl, i, op, "CMP: nothing on stack");

		    if (op.getopt(Opcodes.BITMASK_CMP_CONSUME))
			stackSize--;
		    break;
		default:
		    throw new IllegalOperandException(decl, i, op, "???: unsupported operand");
	    }
	}
    }

    private void validateFunctionCheckLocalUsage(LinkerObjectFile linker, LogicBytecodeFile bytecode, BytecodeFunctionDecl decl) throws BytecodeValidationException {
	if (decl.isnfunc())
	    return;
	FunctionOpcodeList ops = decl.opcodes();

	int startFuncLocals = decl.numargs(),
		numFuncLocals = decl.numlocals();

	Stack< Pair<Integer, List<Boolean>>> localStack = new Stack<>();
	localStack.push(new Pair<>(startFuncLocals, new LinkedList<>()));

	for (int i = 0, j = numFuncLocals; i < j; i++)
	    localStack.peek().b().add(false);

	Pair<Integer, List<Boolean>> obj;
	int idx;

	for (int i = 0, j = ops.size(); i < j; i++) {
	    Opcode op = ops.get(i);
	    Opcodes asEnum = op.opcode();

	    switch (asEnum) {

		case CALLLOCAL:
		case GETLOCAL:
		    int get = (int) op.argobject(0);
		    if (get < startFuncLocals)
			break;
		    obj = localStack.get(idx = (localStack.size() - 1));
		    while (get < obj.a())
			obj = localStack.get(--idx);
		    if (!obj.b().get(get - obj.a()))
			throw new IllegalOperandException(decl, i, op, "dereference of undefined local");
		    break;

		case INCLOSURE:
		    obj = localStack.peek();
		    localStack.push(new Pair<>(obj.a() + obj.b().size(), new LinkedList<>()));
		    break;

		case ENDCLOSURE:
		    localStack.pop();
		    break;

		case NEWUPVAR:
		    localStack.peek().b().add(op.getopt(Opcodes.BITMASK_NEWUPVAR_ARRAY));
		    break;

		case NEWARRAY:
		    int arrLocal = (int) op.argobject(1);
		    if (arrLocal < startFuncLocals)
			break;
		    obj = localStack.get(idx = (localStack.size() - 1));
		    while (arrLocal < obj.a())
			obj = localStack.get(--idx);
		    obj.b().set(arrLocal - obj.a(), true);
		    break;

		case SETLOCAL:
		    int local = (int) op.argobject(0);
		    if (local < startFuncLocals)
			break;
		    obj = localStack.get(idx = (localStack.size() - 1));
		    while (local < obj.a())
			obj = localStack.get(--idx);
		    obj.b().set(local - obj.a(), true);
		    break;

		default:
		    break;

	    }
	}
    }

}
