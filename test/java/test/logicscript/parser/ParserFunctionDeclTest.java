package test.logicscript.parser;

import java.io.StringReader;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionDecl.FunctionDeclType;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.expr.LiteralValue;
import logicscript.ast.nodes.stmt.Statement;
import logicscript.ast.nodes.stmt.ef.SetVariableStatement;
import logicscript.parser.LogicScriptParser;
import logicscript.parser.ParseException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Perform basic parser &amp; AST checks.
 *
 * @author Lochie
 */
public class ParserFunctionDeclTest {

    protected Chunk parse(String s) throws ParseException {
	StringReader input = new StringReader(s);
	LogicScriptParser parser = new LogicScriptParser(input);
	Chunk dstChunk = new Chunk();
	parser.SCRIPT(dstChunk);
	return dstChunk;
    }

    @Test
    public void testUserFuncDecl() {
	Chunk parsed = null;
	try {
	    parsed = parse(
		    "function TestFunctionName "
		    + "takes  integer param0, integer param1, real[] param2, handle[] param3 "
		    + "returns nothing\r\n"
		    + "  local integer local0 = 0\r\n"
		    + "  set local0 = 1\r\n"
		    + "endfunction");
	} catch (ParseException e) {
	    assertNull("expected no exception", e);
	}

	/* Check generated chunk */
	assertNotNull("expected user function declarations", parsed.ufuncDecls);
	assertNull("expected no native function declarations", parsed.nfuncDecls);

	assertEquals("expected 1 user function declaration", 1, parsed.ufuncDecls.size());

	final FunctionDecl func = parsed.ufuncDecls.get(0);

	/* Check function properties */
	assertEquals("expected func.type = USER", FunctionDeclType.USER, func.type);
	assertEquals("expected func.name = 'TestFunctionName'", "TestFunctionName", func.name);

	/* Check function arguments */
	assertNotNull("expected function arguments", func.paramTypes);
	assertEquals("expected 4 function argument types", 4, func.paramTypes.size());

	/* Check function argument types */
	assertEquals("expected argument type in slot 0 to be integer", TypeDecl.TYPE_INTEGER, func.paramTypes.get(0));
	assertEquals("expected argument array type in slot 0 to be false", false, func.paramArrays.get(0));

	assertEquals("expected argument type in slot 1 to be integer", TypeDecl.TYPE_INTEGER, func.paramTypes.get(1));
	assertEquals("expected argument array type in slot 1 to be false", false, func.paramArrays.get(1));

	assertEquals("expected argument type in slot 2 to be real", TypeDecl.TYPE_REAL, func.paramTypes.get(2));
	assertEquals("expected argument array type in slot 2 to be true", true, func.paramArrays.get(2));

	assertEquals("expected argument type in slot 3 to be handle", TypeDecl.TYPE_HANDLE, func.paramTypes.get(3));
	assertEquals("expected argument array type in slot 3 to be true", true, func.paramArrays.get(3));

	/* Check function return type */
	assertEquals("expected nothing return type", TypeDecl.TYPE_NOTHING, func.returnType);
	assertEquals("expected return type array flag to be false", false, func.arrayReturnType);

	/* Check function locals */
	assertNotNull("expected function locals", func.lvars);
	assertEquals("expected 1 function local", 1, func.lvars.size());

	/* Check function local */
	final FunctionLVarDecl lvar = func.lvars.get(0);

	assertEquals("expected local variable name = `local0`", "local0", lvar.nameOfLocalVar);
	assertEquals("expected local array flag is false", false, lvar.isArrayType);
	assertEquals("expected local variable type is integer", TypeDecl.TYPE_INTEGER, lvar.valueType);
	assertNotNull("expected local has initialization expression", lvar.initialRhs);

	final Expression lvarRhs = lvar.initialRhs;
	assertTrue("lvar.initialRhs not instanceof LiteralValue", lvarRhs instanceof LiteralValue);

	final LiteralValue lvarRhsAsLiteral = (LiteralValue) lvarRhs;
	assertEquals("expected lvar.initialRhs is integer literal", TypeDecl.TYPE_INTEGER, lvarRhsAsLiteral.typeOfValue);
	assertEquals("expected lvar.initialRhs is integer value '0'", 0, lvarRhsAsLiteral.valueObject);

	/* Check function block */
	assertNotNull("expected function block", func.block);
	assertEquals("expected 1 statement in function block", 1, func.block.size());

	/* Check single statement */
	final Statement stmt = func.block.get(0);
	assertTrue("func.block[0] not instanceof SetStatement", stmt instanceof SetVariableStatement);

	/* Check SET statement */
	final SetVariableStatement stmtAsSetVar = (SetVariableStatement) stmt;
	assertEquals("expected stmt.nameOfLocalVar = 'local0'", "local0", stmtAsSetVar.nameOfLocalVar);

	/* Check SET expression */
	final Expression setRhs = stmtAsSetVar.rhs;
	assertTrue("expected stmt.rhs instanceof LiteralValue", setRhs instanceof LiteralValue);

	final LiteralValue setRhsAsLiteral = (LiteralValue) setRhs;
	assertEquals("expected stmt.rhs is integer literal", TypeDecl.TYPE_INTEGER, setRhsAsLiteral.typeOfValue);
	assertEquals("expected stmt.rhs is integer value 1", 1, setRhsAsLiteral.valueObject);
    }

    @Test
    public void testNativeFuncDecl() {
	Chunk parsed = null;
	try {
	    parsed = parse("native function TestNativeFunction takes integer param0, real param1, handle[] param2, any param3 returns real");
	} catch (ParseException e) {
	    assertNull("expected no exception", e);
	}

	/* Check generated chunk */
	assertNull("expected no user function declarations", parsed.ufuncDecls);
	assertNotNull("expected native function declarations", parsed.nfuncDecls);

	assertEquals("expected 1 native function declaration", 1, parsed.nfuncDecls.size());

	/* Check function properties */
	final FunctionDecl func = parsed.nfuncDecls.get(0);
	assertEquals("expected func.type = NATIVE", FunctionDeclType.NATIVE, func.type);
	assertEquals("expected func.name = 'TestNativeFunction'", "TestNativeFunction", func.name);

	/* Check function arguments */
	assertNotNull("expected function arguments", func.paramTypes);
	assertEquals("expected 4 function arguments", 4, func.paramTypes.size());

	/* Check function argument types */
	assertEquals("expected argument type in slot 0 to be integer", TypeDecl.TYPE_INTEGER, func.paramTypes.get(0));
	assertEquals("expected argument array type in slot 0 to be false", false, func.paramArrays.get(0));

	assertEquals("expected argument type in slot 1 to be real", TypeDecl.TYPE_REAL, func.paramTypes.get(1));
	assertEquals("expected argument array type in slot 1 to be false", false, func.paramArrays.get(1));

	assertEquals("expected argument type in slot 2 to be handle", TypeDecl.TYPE_HANDLE, func.paramTypes.get(2));
	assertEquals("expected argument array type in slot 2 to be true", true, func.paramArrays.get(2));

	assertEquals("expected argument type in slot 3 to be any", TypeDecl.TYPE_ANY, func.paramTypes.get(3));
	assertEquals("expected argument array type in slot 3 to be false", false, func.paramArrays.get(3));

	/* Check function return type */
	assertEquals("expected real return type", TypeDecl.TYPE_REAL, func.returnType);
	assertEquals("expected return type array flag to be false", false, func.arrayReturnType);

	/* Sanity checks */
	assertNull("expected no function locals", func.lvars);
	assertNull("expected no function block", func.block);
    }

}
