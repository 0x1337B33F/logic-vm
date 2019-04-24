package test.logicscript.vm;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.Opcodes;
import logicbyte.loader.DynamicLibraryImpl;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMFunctionPointer;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMInvokableAccess;
import logicbyte.vm.exception.VMException;
import logicbyte.vm.runtime.BaseVMJFunctionImpl;
import logicbyte.vm.runtime.factory.BaseVMClosureFactory;
import logicbyte.vm.runtime.factory.BaseVMFactory;
import logictype.ThrowObjectException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Lochie
 */
public class VMTest {

    @Before
    public void setUp() {

    }

    @Test
    public void testBasic() {

	DynamicLibraryImpl lib = null;
	LogicVM vm = null;

	final VMDebugCallback callback = new VMDebugCallback() {
	    @Override
	    public void enterfunc(VMInvokable executor, VMFunction caller, VMFunction callee, Opcode generatingOp) {
		System.out.println(String.
			format("enterfunc: %s => %s (%s)", (caller != null) ? caller.name() : "<void>", callee.name(),
				(generatingOp != null) ? generatingOp.opcode() : "<void>"));
	    }

	    @Override
	    public void exitfunc(VMInvokable executor, VMFunction caller, VMFunction callee, Object returnValue) {
		System.out.println(String.format("exitfunc: %s <= %s (return `%s`)", (caller != null) ? caller.name() : "<void>",
			callee.name(), (returnValue != null) ? returnValue : "<void>"));
	    }

	    @Override
	    public void unwindfunc(VMInvokable executor, VMFunction caller, VMFunction callee, Object raiseValue) {
		System.out.println(String.format("unwindfunc: %s <= %s (raising `%s`)", (caller != null) ? caller.name() : "<void>",
			callee.name(), raiseValue));
	    }

	    @Override
	    public void beforeop(VMInvokable executor, VMFunction func, Opcode op) {
		System.out.println(String.format("beforeop: func %s, op %s", func.name(), op.opcode()));
		if (op.args() > 0)
		    for (int i = 0, j = op.args(); i < j; i++)
			System.out.println(String.format("\t(%s): %s", i, op.argobject(i).toString()));

		if (op.opcode() == Opcodes.JMP)
		    System.out.println(
			    String.format("\t(comparison register: %s)",
				    executor.access().getFunctionComparisonRegister(
					    executor.access().getCallStackSize() - 1
				    )
			    )
		    );

		if (op.opcode() == Opcodes.BINOP && ((int) op.argobject(0)) == 18) {

		    VMInvokableAccess access = executor.access();
		    final int callSlot = access.getCallStackSize() - 1;
		    final int stackSize = access.getFunctionStackCount(callSlot);

		    System.out.println(
			    String.format(
				    "\t%s <=> %s",
				    access.getFunctionStackValue(callSlot, stackSize - 1),
				    access.getFunctionStackValue(callSlot, stackSize - 2)
			    )
		    );
		}
	    }

	    @Override
	    public void breakpoint(VMInvokable executor, VMFunction func, Opcode breakpoint) {
		System.out.println(String.format("breakpoint: func %s, op %s", func.name(), breakpoint.opcode()));
	    }

	    @Override
	    public void afterop(VMInvokable executor, VMFunction func, Opcode op) {
		System.out.println(String.format("afterop: func %s, op %s", func.name(), op.opcode()));
	    }
	};

	try {
	    lib = new DynamicLibraryImpl(new File("out"));
	    lib.loadLinkers();

	    System.out.println(String.format("Loaded linkers: %s", lib.getLoadableList().size()));

	    vm = (new BaseVMFactory()).create(lib, new BaseVMClosureFactory());

	    if (vm == null)
		throw new Exception("vm creation failed");

	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    assertNull("expected no exception", e);
	}

	LinkerObjectFile mainLinker = null;

	try {

	    List<LinkerObjectFile> linkers = lib.getLoadableList();
	    assertTrue("no linkers were loaded", linkers.size() > 0);
	    Iterator<LinkerObjectFile> itx = linkers.iterator();

	    mainLinker = lib.getBytecodeFor(new File("out", "test.o"));

	    if (mainLinker == null)
		throw new Exception("Cannot find test");

	    vm.load(null, mainLinker);

	} catch (VMException initException) {
	    initException.printStackTrace(System.err);

	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    assertNull("expected no exception", e);
	}

	try {
	    vm.defineJFunc("TestNativePassedFunctionPointerNativeCall", BaseVMJFunctionImpl.generateFunctionLambda(
		    (vmObj, exec, debug, args) -> {
			if (args.length == 0)
			    throw new Exception("arguments required to native call");
			VMFunctionPointer pointerRef = (VMFunctionPointer) args[0];
			return exec.tailcall(debug, pointerRef, null, true);
		    }));

	    vm.defineJFunc("TestNativeThrowsExceptionAlways", BaseVMJFunctionImpl.generateFunctionLambda((vmObj, exec, debug, args) -> {
		throw new ThrowObjectException(42);
	    }));

	} catch (ReflectiveOperationException e) {
	    e.printStackTrace(System.err);
	    assertNull("expected no reflection exception", e);
	}

	try {

	    /*
	    VMInvokable callable = vm.invoke(callback, vm.getFunction(mainLinker, "TestNativePassedFunctionPointer000"));
	    callable.invoke(callback, null);
	    while (!callable.advance(callback));

	    callable = vm.invoke(callback, vm.getFunction(mainLinker, "TestSetGlobalArrayIntVal"));
	    callable.invoke(callback, null);
	    while (!callable.advance(callback)) ;

	    callable = vm.invoke(null, vm.getFunction(mainLinker, "TestGetGlobalArrayIntVal"));
	    callable.invoke(callback, null);
	    while (!callable.advance(null));*/
	    VMInvokable callable = vm.invoke(null, vm.getFunction(mainLinker, "TestCoFuncCaptureError"));
	    callable.invoke(null, null);

	    boolean state = false;
	    while (true)
		if (callable.advance(null)) {
		    state = vm.yielded(callable);
		    if (state)
			System.out.println(String.format("yielded: `%s`", vm.yield(callable)));
		    else {
			System.out.println("completed");
			break;
		    }
		}

	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    assertNull("expected no exception", e);
	}
    }

}
