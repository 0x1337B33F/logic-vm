package test.logicscript.vm;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.loader.DynamicLibraryImpl;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMFunctionPointer;
import logicbyte.vm.VMInvokable;
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

	try {
	    lib = new DynamicLibraryImpl(new File("out"));
	    lib.loadLinkers();

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

	    mainLinker = lib.getBytecodeFor(new File("out", "test.ls.o"));

	    if (mainLinker == null)
		throw new Exception("Cannot find test");

	    vm.load(mainLinker);

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
	    VMInvokable callable = vm.invoke(null, vm.getFunction(mainLinker, "TestNativeFunctionThrowsPrimitive"));
	    callable.invoke(null, null);
	    while (!callable.advance(null));
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    assertNull("expected no exception", e);
	}
    }

}
