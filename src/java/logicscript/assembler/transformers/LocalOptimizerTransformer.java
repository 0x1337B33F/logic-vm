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

    }

    private void applyOptimizeLocalUsage(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes) {
	for (int i = 0, j = generatedOpcodes.size(); i < j; i++) {
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
	Iterator<Pair<Object, DebugLocationSegment>> itx = generatedOpcodes.iterator();
	while (itx.hasNext()) {
	    Pair<Object, DebugLocationSegment> op = itx.next();
	    if (op.a() instanceof Opcode) {
		Opcode o = (Opcode) op.a();
		Opcodes asEnum = o.opcode();
		if (asEnum == Opcodes.GETLOCAL || asEnum == Opcodes.GETLOCALARR) {
		    int localSlot = (int) o.argobject(0) - generatedFunc.numargs();
		    localUseList.set(localSlot, localUseList.get(localSlot) + 1);
		}
	    }
	}
    }

}
