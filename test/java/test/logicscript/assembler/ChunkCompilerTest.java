package test.logicscript.assembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicscript.assembler.writer.ChunkAssembler;
import logicscript.ast.Chunk;
import logicscript.parser.LogicScriptParser;
import logicscript.parser.ParseException;
import org.junit.Assert;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Lochie
 */
public class ChunkCompilerTest {

    LogicScriptParser _parserMachine = null;
    InputStream _parserInput = null;
    Chunk _generatedCodeChunk = null;

    @Before
    public void setUp() throws Exception {
	_parserInput = new FileInputStream(new File("test/script/lex/root.ls"));
	_parserMachine = new LogicScriptParser(_parserInput);

	_generatedCodeChunk = new Chunk();

	try {
	    this._parserMachine.SCRIPT(_generatedCodeChunk);
	} catch (ParseException e) {
	    assertNull("Expected no exception", e);
	}

    }

    @Test
    public void testCompilerBasic() {

	ChunkAssembler assembler = new ChunkAssembler();

	LinkerObjectFile allocLinkDst = new LinkerObjectFile();
	LogicBytecodeFile allocBytecodeDst = new LogicBytecodeFile();

	try {
	    assembler.assembleChunk(_generatedCodeChunk, allocLinkDst, allocBytecodeDst);
	} catch (Exception e) {
	    e.printStackTrace(System.out);
	    fail(e.getMessage() + "\r\n" + e.toString());
	}

	Assert.assertNotEquals("no imports written", 0, allocLinkDst.importsTable.size());
	Assert.assertNotEquals("no exports written", 0, allocLinkDst.exportsTable.size());
	Assert.assertNotEquals("no globals written", 0, allocLinkDst.globalsTable.size());
	Assert.assertNotEquals("no types written", 0, allocLinkDst.typesTable.size());

	Assert.assertNotEquals("no functiondecls written", 0, allocBytecodeDst.getAll().length);

	BytecodeFunctionDecl decl = allocBytecodeDst.get("Coroutine_SPushB");

	Assert.assertEquals("expected isufunc=true", true, decl.isufunc());
	Assert.assertEquals("expected isnfunc=false", false, decl.isnfunc());

	Assert.assertEquals("expected numargs=1", 1, decl.numargs());
	Assert.assertEquals("expected numlocals=1", 1, decl.numlocals());

	Assert.assertEquals("expected hasargs=true", true, decl.hasargs());
	Assert.assertEquals("expected hasdebug=true", true, decl.hasdebug());
	Assert.assertEquals("expected haslocals=true", true, decl.haslocals());
	Assert.assertEquals("expected hasreturn=true", true, decl.hasreturn());

	Assert.assertEquals("expected argtype(0)=3", 3, decl.argtype(0));
	Assert.assertEquals("expected returntype=3", 3, decl.returntype());

	Assert.assertEquals("expected EXPORTS{root.math}", true, allocLinkDst.exports("root.math"));

	List<ExportsEntry> exportsByName = allocLinkDst.getexport("root.math");
	Assert.assertEquals("expected exportQuery.size() == 2", 2, exportsByName.size());

	ExportsEntry first = exportsByName.get(0);
	Assert.assertEquals("expected ExportsEntry.type = GLOBALS", LinkerObjectFile.TYPEFLAGS_EXPORTS_GLOBAL, first.typeflags);
	Assert.assertEquals("expected ExportsEntry.labels.size() = 1", 1, first.labels.size());
	Assert.assertEquals("expected ExportsEntry.labels[0] = 'MATH_PI'", "MATH_PI", first.labels.get(0));

	GlobalsEntry piGlobal = allocLinkDst.getglobal(first.labels.get(0));

	Assert.assertEquals("expected root.math.MATH_PI global to be real type", 5, piGlobal.type);

	ExportsEntry second = exportsByName.get(1);
	Assert.assertEquals("expected ExportsEntry.type = GLOBALS", LinkerObjectFile.TYPEFLAGS_EXPORTS_FUNCTION, second.typeflags);
	Assert.assertEquals("expected ExportsEntry.labels.size() = 2", 2, second.labels.size());

	Assert.assertEquals("expected ExportsEntry.labels[0] = 'Math_Rad2Deg'", "Math_Rad2Deg", second.labels.get(0));
	Assert.assertEquals("expected ExportsEntry.labels[1] = 'Math_Deg2Rad'", "Math_Deg2Rad", second.labels.get(1));

	System.out.println(allocBytecodeDst.get("Math_Rad2Deg").toString());

    }

}
