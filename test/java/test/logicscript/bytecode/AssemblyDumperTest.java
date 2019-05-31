package test.logicscript.bytecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.dump.AssemblyDumper;
import logicbyte.bytecode.stream.LinkerObjectInputStream;
import logicbyte.bytecode.stream.LogicBytecodeInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Lochie
 */
public class AssemblyDumperTest {

    LinkerObjectFile dumpLinkerObj;
    LogicBytecodeFile dumpBytecodeObj;

    @Before
    public void setUp() {

	FileInputStream fis = null;

	try {
	    LinkerObjectInputStream is = new LinkerObjectInputStream(fis = new FileInputStream(new File("out", "root.o")), "root");
	    dumpLinkerObj = is.readLinkerObject();
	    fis.close();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	    Assert.assertNull("expected no exception", e);
	}

	try {
	    LogicBytecodeInputStream is = new LogicBytecodeInputStream(fis = new FileInputStream(new File("out", "root.lb")), "root");
	    dumpBytecodeObj = is.readBytecodeFile();
	    fis.close();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	    Assert.assertNull("expected no exception", e);
	}

    }

    @Test
    public void testBasic() {
	final PrintWriter wrapper = new PrintWriter(System.out);

	AssemblyDumper.dump(dumpLinkerObj, wrapper);
	AssemblyDumper.dump(dumpBytecodeObj, wrapper);

	wrapper.flush();
	wrapper.close();

    }

}
