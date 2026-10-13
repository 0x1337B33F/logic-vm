package logicscript.assembler.transformers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugLocationSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDeclMutable;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.OpcodeControlBitmask;
import logicbyte.bytecode.Opcode.Opcodes;
import logicscript.assembler.FunctionBytecodeTransformer;
import logictype.LangUtils;
import logictype.Log;
import logictype.Pair;

/**
 *
 * @author Lochie
 */
public class LocalOptimizerTransformer implements FunctionBytecodeTransformer {

    @Override
    public void apply(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes, Log log) throws Exception {
	if (generatedFunc.isnfunc())
	    return;

	if (generatedFunc.numlocals() == 0)
	    return;

	List<Integer> localUseList = new LinkedList<>();
	applyOptimizeLocalUsage(generatedFunc, generatedFuncDebug, generatedOpcodes, log);
	applyCalculateLocalUsage(generatedFunc, generatedFuncDebug, generatedOpcodes, localUseList, log);
	applyRemoveUnusedLocals(generatedFunc, generatedFuncDebug, generatedOpcodes, localUseList, log);
    }

    private void applyOptimizeLocalUsage(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes, Log log) {
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
		    log.trace("applying DUP-SET to SET->GET and nuking GET at mark %s", i);
		    generatedOpcodes.set(i, new Pair<>(Opcodes.DUP, obj1.b()));
		    generatedOpcodes.set(i + 1, obj0);
		}
	    }
	}
    }

    private void applyCalculateLocalUsage(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes, List<Integer> localUseList, Log log) {
	final int numLocals = generatedFunc.numlocals();
	Iterator<Pair<Object, DebugLocationSegment>> itx = generatedOpcodes.iterator();
	while (itx.hasNext()) {
	    Pair<Object, DebugLocationSegment> op = itx.next();
	    if (op.a() instanceof Opcode) {
		Opcode o = (Opcode) op.a();
		Opcodes asEnum = o.opcode();
		if (asEnum == Opcodes.GETLOCAL || asEnum == Opcodes.GETLOCALARR || asEnum == Opcodes.CALLLOCAL)
		    if (!localUseList.contains((int) o.argobject(0)))
			localUseList.add((int) o.argobject(0));
	    }
	}
    }

    private void applyRemoveUnusedLocals(BytecodeFunctionDeclMutable generatedFunc, DebugSegment generatedFuncDebug,
	    List<Pair<Object, DebugLocationSegment>> generatedOpcodes, List<Integer> localUseList, Log log) throws Exception {

	// gobble memory, possibly unboundedly?:
	int[] remapBuf = new int[generatedFunc.numlocals()];
	for (int i = 0, j = remapBuf.length; i < j; i++)
	    remapBuf[i] = i;

	int deleted = 0;
	for (int i = (generatedFunc.numlocals()) - 1, j = i; i >= 0; i--) {
	    if (localUseList.contains(i))
		continue;

	    log.debug("removing local %s from func `%s`", i, generatedFunc.name());

	    // if there are entries to the right of the current deletion...
	    if (i < remapBuf.length - 2)
		// for remap purposes, everything to the /right/ of the deletion
		// is now shifted to the left:
		for (int k = i, l = remapBuf.length - 1; k < l; k++)
		    remapBuf[k] = remapBuf[k + 1];

	    for (int k = (generatedOpcodes.size() - 1); k >= 0; k--) {
		Pair<Object, DebugLocationSegment> obj = generatedOpcodes.get(k);
		if (obj.a() instanceof Opcode) {
		    Opcode o = (Opcode) obj.a();
		    Opcodes op = o.opcode();

		    if (op == Opcodes.SETLOCAL && (int) o.argobject(0) == i)
			generatedOpcodes.remove(k);

		    if (op == Opcodes.SETLOCALARR && (int) o.argobject(0) == i)
			generatedOpcodes.remove(k);

		    if (op == Opcodes.NEWARRAY && (int) o.argobject(1) == i)
			generatedOpcodes.remove(k);
		}
	    }
	    deleted++;
	}

	if (deleted > 0) // if we did something...
	    if (deleted == generatedFunc.numlocals()) { // ... and it was completely apocalyptic...
		// nuke all locals, don't bother doing remapping (not required)
		generatedFunc.setlocals(null, null);
		generatedFuncDebug.setlocalnames(null);
	    } else { // ... only minor earthquake
		// defragment localtypes storage:
		final int heapSize = generatedFunc.numlocals() - deleted;
		int[] newlocaltypes = new int[heapSize];
		boolean[] newlocalarrays = new boolean[heapSize];
		String[] newlocalnames = new String[heapSize];
		int ptr = 0;
		for (int i = 0; i < heapSize; i++) {
		    if (localUseList.get(i) == 0)
			continue;
		    newlocaltypes[ptr] = generatedFunc.localtype(i);
		    newlocalarrays[ptr] = generatedFunc.localarray(i);
		    newlocalnames[ptr] = generatedFuncDebug.localname(i);
		    ptr++;
		}
		generatedFunc.setlocals(newlocaltypes, newlocalarrays);
		generatedFuncDebug.setlocalnames(newlocalnames);
		// perform remapping: for each access to an lvar, left-to-right scan
		// remapBuf to find the value in the collection; when found, the index
		// of the value in the array reveals the new lvar assignment:

		Pair<Object, DebugLocationSegment> ptr1 = null;
		for (int i = 0, j = generatedOpcodes.size(); i < j; i++) {
		    ptr1 = generatedOpcodes.get(i);
		    if (ptr1.a() instanceof Opcode) {
			Opcode o = (Opcode) ptr1.a();
			Opcodes op = o.opcode();

			int slot = -1;

			switch (op) {
			    case CALLLOCAL:
			    case GETLOCAL:
			    case SETLOCAL:
			    case GETLOCALARR:
			    case SETLOCALARR: {
				slot = (int) o.argobject(0);
			    }
			    break;
			    default: {
				/* voidable */ }
			}

			if (slot == -1)
			    continue;

			for (int k = 0, l = remapBuf.length - deleted; k < l; k++)
			    if (remapBuf[k] == slot) {
				OpcodeControlBitmask ptr2 = null;
				LinkedList<Object> ptr3 = new LinkedList<>();
				for (int m = 0, n = o.args(); m < n; m++)
				    ptr3.add(o.argobject(m));
				// FIXME: this assumption is dangerous; the lvar slot may not be the first argobj
				ptr3.set(0, k);
				if (o.hasopts())
				    try {
					ptr2 = new OpcodeControlBitmask(o.opcode(), o.getopts(), ptr3);
				    } catch (InstantiationException ie) {
					throw new Exception("Transformer has corrupted code, abort", ie);
				    }
				else
				    try {
					ptr2 = new OpcodeControlBitmask(o.opcode(), ptr3);
				    } catch (InstantiationException ie) {
					throw new Exception("Transformer has corrupted code, abort", ie);
				    }
				// FIXME: technically, the "debug location" has changed, since we have re-written
				// the opcode; do we need to indicate that we have been here?
				generatedOpcodes.set(i, new Pair<>(ptr2, generatedOpcodes.get(i).b()));
			    }
		    }
		}
	    } // FIXME: we do need to renumber the closures as part of the optimize step,
	// however, this is as simple as changing the beginning offset address, slide
	// all values as far left as they can go, then do the same offset shift on 
	// all the other instructions which rely on upvars. (the upvars cannot be
	// removed because they are required for the control-flow portion of the
	// particular statement(s) which they were created for, even if they are
	// functionally unused in the declarative Block)
    }

}
