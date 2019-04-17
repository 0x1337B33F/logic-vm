package test.logicscript.assembler.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import logicscript.assembler.validation.ReturnValidationVisitor;
import logicscript.assembler.validation.ReturnValidationVisitor.FunctionReturnPathInfo;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.stmt.cfl.ReturnStatement;
import logicscript.parser.LogicScriptParser;
import logicscript.parser.ParseException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ReturnValidationVisitorTests {

    LogicScriptParser _parserMachine = null;
    InputStream _parserInput = null;
    Chunk _generatedCodeChunk = null;

    @Before
    public void setUp() throws Exception {
	_parserInput = new FileInputStream(new File("test/script/assembler/validation/funcret.ls"));
	_parserMachine = new LogicScriptParser(_parserInput);
	_generatedCodeChunk = new Chunk();
	
	try {
	    this._parserMachine.SCRIPT( _generatedCodeChunk );
	} catch (ParseException e) {
	    e.printStackTrace(System.err);
	    assertNull("Expected no exception", e);
	}
    }

    @Test
    public void testVisitChunkReturnStatements() {
	
	ReturnValidationVisitor visitor = new ReturnValidationVisitor();
	try {
	    visitor.visitChunk(_generatedCodeChunk);
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    assertNull("Expected no exception", e);
	}
	
	Map<FunctionDecl, FunctionReturnPathInfo> funcRetMap = visitor.getFunctionReturnPathTable();
	assertFalse("did not generate function returns map", funcRetMap.isEmpty());
	
	Iterator<FunctionDecl> itx = _generatedCodeChunk.ufuncDecls.iterator();
	while (itx.hasNext()) {
	    FunctionDecl decl = itx.next();
	    FunctionReturnPathInfo returnsInFunction = funcRetMap.get(decl);
	    List<ReturnStatement> returnsList = returnsInFunction.returnStatements;
	    assertNotNull(String.format("did not generate function returns list for function %s", decl.name), returnsList);
	    if (decl.returnType != TypeDecl.TYPE_NOTHING)
		assertNotEquals(String.format(
			"on non-nothing return type %s, did not find any RETURNS statement; expected validation failure", decl.returnType),
			0, returnsList.size());
	    
	    
	    System.out.println(String.format("\r\n\r\nRETURNS information about user function `%s` (returns  %s):", decl.name, decl.returnType));
	    System.out.println(String.format("\tcount RETURNS: %s", returnsList.size()));
	    Iterator<ReturnStatement> itx1 = returnsList.iterator();
	    while ( itx1.hasNext())  {
		ReturnStatement exitNode = itx1.next();
		System.out.println(String.format("\t\t node: %s\t(expr: %s)\t(at %s)", exitNode, exitNode.returnExpression, exitNode.location));
	    }
	}
    }

}
