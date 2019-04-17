package test.logicscript.assembler.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import logicscript.assembler.validation.LoopValidationVisitor;
import logicscript.assembler.validation.LoopValidationVisitor.LoopExitPathInfo;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.stmt.cfl.ExitWhenStatement;
import logicscript.ast.nodes.stmt.cfl.ReturnStatement;
import logicscript.parser.LogicScriptParser;
import logicscript.parser.ParseException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Lochie
 */
public class LoopValidationVisitorTests {

    LogicScriptParser _parserMachine = null;
    InputStream _parserInput = null;
    Chunk _generatedCodeChunk = null;

    @Before
    public void setUp() throws Exception {
	_parserInput = new FileInputStream(new File("test/script/assembler/validation/loops.ls"));
	_parserMachine = new LogicScriptParser(_parserInput);
	_generatedCodeChunk = new Chunk();

	try {
	    this._parserMachine.SCRIPT(_generatedCodeChunk);
	} catch (ParseException e) {
	    e.printStackTrace(System.err);
	    assertNull("Expected no exception", e);
	}
    }

    @Test
    public void testVisitChunkLoopStatements() {

	LoopValidationVisitor visitor = new LoopValidationVisitor();
	try {
	    visitor.visitChunk(_generatedCodeChunk);
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    assertNull("Expected no exception", e);
	}

	Map<FunctionDecl, List<LoopExitPathInfo>> funcRetMap = visitor.getFunctionLoopsTable();
	assertFalse("did not generate function loops map", funcRetMap.isEmpty());

	Iterator<FunctionDecl> itx = _generatedCodeChunk.ufuncDecls.iterator();
	while (itx.hasNext()) {
	    FunctionDecl decl = itx.next();

	    List<LoopExitPathInfo> loopsInFunction = funcRetMap.get(decl);

	    if (loopsInFunction != null) {
		Iterator<LoopExitPathInfo> itx1 = loopsInFunction.iterator();
		while (itx1.hasNext()) {
		    LoopExitPathInfo loopInst = itx1.next();
		    List<ExitWhenStatement> exitsList = loopInst.exitStatements;
		    List<ReturnStatement> returnsList = loopInst.returnStatements;

		    assertNotNull(String.format("did not generate loops exits list for function %s, loop at %s", decl.name,
			    loopInst.loopBlock.location), exitsList);
		    assertNotNull(String.format("did not generate loops returns list for function %s, loop at %s", decl.name,
			    loopInst.loopBlock.location), returnsList);

		    System.out.println(String.format("\r\n\r\nLOOP information about user function `%s` (returns  %s) loop at %s:",
			    decl.name,
			    decl.returnType, loopInst.loopBlock.location));

		    System.out.println(String.format("\tcount EXITS: %s", exitsList.size()));
		    System.out.println(String.format("\tcount RETURNS: %s", returnsList.size()));

		    Iterator<ExitWhenStatement> itx2 = exitsList.iterator();
		    while (itx2.hasNext()) {
			ExitWhenStatement exitNode = itx2.next();
			System.out.println(String.format("\t\t node: %s\t(expr: %s)\t(at %s)", exitNode, exitNode.condExitWhenExpr,
				exitNode.location));
		    }

		    Iterator<ReturnStatement> itx3 = returnsList.iterator();
		    while (itx3.hasNext()) {
			ReturnStatement exitNode = itx3.next();
			System.out.println(String.format("\t\t node: %s\t(expr: %s)\t(at %s)", exitNode, exitNode.returnExpression,
				exitNode.location));
		    }
		}
	    }
	}

    }
}
