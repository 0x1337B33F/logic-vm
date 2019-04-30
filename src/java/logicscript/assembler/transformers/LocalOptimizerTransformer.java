package logicscript.assembler.transformers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugLocationSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDeclMutable;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.Opcodes;
import logicscript.assembler.FunctionBytecodeTransformer;
import logictype.Pair;

/**
 *
 * @author Lochie
 */
public class LocalOptimizerTransformer implements FunctionBytecodeTransformer {

    @Override
    public void apply(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes) {
	if (generatedFunc.isnfunc())
	    return;

	List<Integer> localUseList = new LinkedList<>();
	for (int i = 0, j = generatedFunc.numlocals(); i < j; i++)
	    localUseList.add(0);

	applyOptimizeLocalUsage(generatedFunc, generatedFuncDebug, generatedOpcodes);
	applyCalculateLocalUsage(generatedFunc, generatedFuncDebug, generatedOpcodes, localUseList);
	applyRemoveUnusedLocals(generatedFunc, generatedFuncDebug, generatedOpcodes, localUseList);
    }

    private void applyOptimizeLocalUsage(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes) {
	for (int i = 0, j = generatedOpcodes.size() - 1; i < j; i++) {
	    Pair<Object, DebugLocationSegment> obj0 = generatedOpcodes.get(i),
		    obj1 = generatedOpcodes.get(i + 1);

	    if (obj0.a() instanceof Opcode) {
		Opcode o0 = (Opcode) obj0.a(), o1 = null;
		Opcodes asEnum0 = o0.opcode(), asEnum1 = null;

		if (obj1.a() instanceof Opcode) {
		    o1 = (Opcode) obj1.a();
		    asEnum1 = o1.opcode();
		}

		if (asEnum0 == Opcodes.NOP || o1 == null)
		    continue;

		if (asEnum0 == Opcodes.SETLOCAL && asEnum1 == Opcodes.GETLOCAL && (int) o0.argobject(0) == (int) o1.argobject(0)) {
		    generatedOpcodes.set(i, new Pair<>(Opcodes.DUP, obj1.b()));
		    generatedOpcodes.set(i + 1, obj0);
		}
	    }
	}
    }

    private void applyCalculateLocalUsage(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes, List<Integer> localUseList) {
	// FIXME: must recurse into INCLOSURE / ENDCLOSURE structure to properly calculate
	// usage of locals within closure scope. Woops.
	Iterator<Pair<Object, DebugLocationSegment>> itx = generatedOpcodes.iterator();
	final int numArgs = generatedFunc.numargs();
	while (itx.hasNext()) {
	    Pair<Object, DebugLocationSegment> op = itx.next();
	    if (op.a() instanceof Opcode) {
		Opcode o = (Opcode) op.a();
		Opcodes asEnum = o.opcode();
		if (asEnum == Opcodes.GETLOCAL || asEnum == Opcodes.GETLOCALARR) {
		    int slot = (int) o.argobject(0);
		    if (slot >= numArgs) {
			int localSlot = (int) o.argobject(0) - numArgs;
			localUseList.set(localSlot, localUseList.get(localSlot) + 1);
		    }
		}
	    }
	}
    }

    private void applyRemoveUnusedLocals(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes, List<Integer> localUseList) {

	int deleted = 0;
	for (int i = (generatedFunc.numlocals()) - 1; i >= 0; i--) {
	    if (localUseList.get(i) != 0)
		continue;
	    int slot = generatedFunc.numargs() + i;
	    int stack = 0;

	    for (int j = (generatedOpcodes.size() - 1); j >= 0; j--) {
		Pair<Object, DebugLocationSegment> obj = generatedOpcodes.get(j);
		if (obj.a() instanceof Opcode) {
		    Opcode o = (Opcode) obj.a();
		    Opcodes op = o.opcode();
		    if (stack > 0) {
			generatedOpcodes.remove(j);

			switch (op) {
			    case BINOP:
				/* 2 stack values -> 1 stack value */
				stack++;
			    case UNOP:
				/* 1 stack value -> 1 stack value */
				break;
			    case CALL:
				/* n stack values -> 0|1 stack value */
				if (o.getopt(Opcodes.BITMASK_CALL_RVAL))
				    stack--;
				stack += (int) o.argobject(1);
				break;
			    case CALLLOCAL:
				/* n stack values -> 0|1 stack value */
				if (o.getopt(Opcodes.BITMASK_CALLLOCAL_RVAL))
				    stack--;
				stack += (int) o.argobject(1);
				break;
			    case CALLGLOBAL:
				/* n stack values -> 0|1 stack value */
				if (o.getopt(Opcodes.BITMASK_CALLGLOBAL_RVAL))
				    stack--;
				stack += (int) o.argobject(1);
				break;
			    case FUNCPTR:
				/* arg funcname -> 1 stack value */
				stack--;
				break;

			    case POP:
				/* -1 stack value */
				stack++;
				break;
			    case DUP:
				/* 1 stack value */
				stack--;
				break;
			    case GETGLOBALARR:
			    case GETLOCALARR:
				/* 1 stack value -> 1 stack value */
				break;

			    case GETGLOBAL:
			    case GETLOCAL:
			    case PUSHB:
			    case PUSHI:
			    case PUSHR:
			    case PUSHS:
				/* -> 1 stack value */
				stack--;
				break;
			}

		    } else {

			// op used for literal set, literal init (stack = value)
			if (op == Opcodes.SETLOCAL && (int) o.argobject(0) == slot) {
			    stack++;
			    generatedOpcodes.remove(j);
			}

			// op used for array set (stack = index, value)
			if (op == Opcodes.SETLOCALARR && (int) o.argobject(0) == slot) {
			    stack += 2;
			    generatedOpcodes.remove(j);
			}

			// op used for array init (stack = dimension)
			if (op == Opcodes.NEWARRAY && (int) o.argobject(1) == slot) {
			    stack++;
			    generatedOpcodes.remove(j);
			}
		    }
		}
	    }
	    deleted++;
	}

	if (deleted == generatedFunc.numlocals()) {
	    generatedFunc.setlocals(null, null);
	    generatedFuncDebug.setlocalnames(null);
	} else {
	    int[] newlocaltypes = new int[ generatedFunc.numlocals() - deleted ];
	    boolean[] newlocalarrays = new boolean[ generatedFunc.numlocals() - deleted ];
	    String[] newlocalnames = new String[ generatedFunc.numlocals() - deleted ];
	    int ptr = 0;
	    for (int i = 0, j = generatedFunc.numlocals(); i < j; i++) {
		if (localUseList.get(i) == 0)
		    continue;
		newlocaltypes[ptr] = generatedFunc.localtype(i);
		newlocalarrays[ptr] = generatedFunc.localarray(i);
		newlocalnames[ptr] = generatedFuncDebug.argname(i);
		ptr++;
	    }
	    generatedFunc.setlocals(newlocaltypes, newlocalarrays);
	    generatedFuncDebug.setlocalnames(newlocalnames);
	}

    }

}
