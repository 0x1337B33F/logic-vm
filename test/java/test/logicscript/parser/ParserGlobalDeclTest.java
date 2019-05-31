package test.logicscript.parser;

import java.io.StringReader;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.LiteralValue;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.parser.LogicScriptParser;
import logicscript.parser.ParseException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Lochie
 */
public class ParserGlobalDeclTest {

    protected Chunk parse(String s) throws ParseException {
	StringReader input = new StringReader(s);
	LogicScriptParser parser = new LogicScriptParser(input);
	Chunk dstChunk = new Chunk();
	parser.SCRIPT(dstChunk);
	return dstChunk;
    }

    @Test
    public void testGlobalsDecl() {

	Chunk parsed = null;
	try {
	    parsed = parse("globals\r\n"
		    + "  integer TestGlobalInt000 = 42\r\n"
		    + "  integer TestGlobalInt001 = 1, TestGlobalInt002 = 2, TestGlobalInt003 = 3\r\n"
		    + "  array real TestGlobalRealArray000[ 8 ]\r\n"
		    + "  array handle TestGlobalHandleArray000 [ 4 ]\r\n"
		    + "endglobals");
	} catch (ParseException e) {
	    assertNull("expected no exception", e);
	}

	/* Check generated chunk */
	assertNotNull("expected globals declarations", parsed.globalsDecls);
	assertEquals("expected 1 globals declaration block", 1, parsed.globalsDecls.size());

	/* Check generated globals block */
	final GlobalsBlock globals = parsed.globalsDecls.get(0);
	GlobalsVarDecl decl = null;
	LiteralValue lv = null;
	for (int i = 0; i < 4; i++) {
	    decl = globals.get(i);
	    String nameOfGlobal = String.format("TestGlobalInt00%s", i);
	    assertEquals(String.format("expected decl.nameOfGlobalVar = '%s'", nameOfGlobal), nameOfGlobal, decl.nameOfGlobalVar);
	    assertEquals(String.format("expected global in index %s to be integer", i), TypeDecl.TYPE_INTEGER, decl.valueType);
	    assertEquals(String.format("expected global in index %s to be literal type, got array", i), false, decl.isArrayType);

	    assertTrue("expected literal value for initialization expression", decl.rhs instanceof LiteralValue);
	    lv = (LiteralValue) decl.rhs;
	    assertEquals("expected literal value to be integer type", TypeDecl.TYPE_INTEGER, lv.typeOfValue);
	    if (i == 0)
		assertEquals("expected literal value to be value '42'", 42, lv.valueObject);
	    else
		assertEquals(String.format("expected literal value to be value '%s'", i), i, lv.valueObject);
	}

	decl = globals.get(4);
	assertEquals("expected decl.nameOfGlobalVar = 'TestGlobalRealArray000'", "TestGlobalRealArray000", decl.nameOfGlobalVar);
	assertEquals("expected decl.valueType = REAL", TypeDecl.TYPE_REAL, decl.valueType);
	assertEquals("expected decl.isArrayType = true", true, decl.isArrayType);
	assertTrue("expected literal value for array dimension expression", decl.rhs instanceof LiteralValue);
	lv = (LiteralValue) decl.rhs;
	assertEquals("expected literal value to be integer type", TypeDecl.TYPE_INTEGER, lv.typeOfValue);
	assertEquals("expected literal value to be value '8'", 8, lv.valueObject);

	decl = globals.get(5);
	assertEquals("expected decl.nameOfGlobalVar = 'TestGlobalHandleArray000'", "TestGlobalHandleArray000", decl.nameOfGlobalVar);
	assertEquals("expected decl.valueType = HANDLE", TypeDecl.TYPE_HANDLE, decl.valueType);
	assertEquals("expected decl.isArrayType = true", true, decl.isArrayType);
	assertTrue("expected literal value for array dimension expression", decl.rhs instanceof LiteralValue);
	lv = (LiteralValue) decl.rhs;
	assertEquals("expected literal value to be integer type", TypeDecl.TYPE_INTEGER, lv.typeOfValue);
	assertEquals("expected literal value to be value '4'", 4, lv.valueObject);

    }

}
