package test.logicscript.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import logicscript.ast.visitors.PrintTreeVisitor;
import logicscript.ast.Chunk;
import logicscript.parser.LogicScriptParser;
import logicscript.parser.ParseException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ParserBaseTests {

    private LogicScriptParser testParser = null;
    private InputStream testInput = null;

    @Before
    public void setUp() throws Exception {
	testInput = new FileInputStream(new File("../test/script/lex/test.ls"));
	testParser = new LogicScriptParser(testInput);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSCRIPT() {
	Chunk destinationObj = new Chunk();

	try {
	    testParser.SCRIPT(destinationObj);
	} catch (ParseException e) {
	    assertNull("Expected no exception", e);
	}

	assertNotNull("IMPORTS not parsed !", destinationObj.libImportsDecls);
	assertNotNull("EXPORTS not parsed !", destinationObj.libExportsDecls);
	assertNotNull("GLOBALS not parsed !", destinationObj.globalsDecls);
	assertNotNull("TYPE not parsed !", destinationObj.utypeDecls);
	assertNotNull("NATIVE FUNCTION not parsed !", destinationObj.nfuncDecls);
	assertNotNull("FUNCTION not parsed !", destinationObj.ufuncDecls);

	try {
	    destinationObj.visit(new PrintTreeVisitor());
	} catch (Exception e) {
	    assertNull("Expected no exception", e);
	}

	System.out.println("---- Stats for nerds: ");
	System.out.println(String.format("Number of IMPORTS: %s", destinationObj.libImportsDecls.size()));
	System.out.println(String.format("Number of EXPORTS: %s", destinationObj.libExportsDecls.size()));
	System.out.println(String.format("Number of GLOBALS: %s", destinationObj.globalsDecls.size()));
	System.out.println(String.format("Number of TYPEs: %s", destinationObj.utypeDecls.size()));
	System.out.println(String.format("Number of NATIVE FUNCTIONs: %s", destinationObj.nfuncDecls.size()));
	System.out.println(String.format("Number of user FUNCTIONs: %s", destinationObj.ufuncDecls.size()));

    }

}
