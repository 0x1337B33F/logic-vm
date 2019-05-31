package test.logicscript.parser;

import java.io.StringReader;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.decl.TypeDecl.TypeDeclType;
import logicscript.parser.LogicScriptParser;
import logicscript.parser.ParseException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 *
 * @author Lochie
 */
public class ParserTypeDeclTest {

    protected Chunk parse(String s) throws ParseException {
	StringReader input = new StringReader(s);
	LogicScriptParser parser = new LogicScriptParser(input);
	Chunk dstChunk = new Chunk();
	parser.SCRIPT(dstChunk);
	return dstChunk;
    }

    @Test
    public void testFirstDegreeTypeDecls() {

	Chunk parsed = null;
	try {
	    parsed = parse("type alpha extends handle\r\ntype beta extends handle");
	} catch (ParseException e) {
	    assertNull("expected no exception", e);
	}

	assertNotNull("expected type declarations", parsed.utypeDecls);
	assertEquals("expected 2 type declarations", 2, parsed.utypeDecls.size());

	/* Check `alpha` typedecl */
	final TypeDecl declA = parsed.utypeDecls.get(0);
	assertEquals("expected declA.name = 'alpha'", "alpha", declA.typeName);
	assertEquals("expected declA.type = EXTENDS_HANDLE", TypeDeclType.EXTENDS_HANDLE, declA.type);
	assertNull("expected declA.ancestorTypeName = null", declA.ancestorTypeName);

	/* Check `beta` typedecl */
	final TypeDecl declB = parsed.utypeDecls.get(1);
	assertEquals("expected declA.name = 'beta'", "beta", declB.typeName);
	assertEquals("expected declA.type = EXTENDS_HANDLE", TypeDeclType.EXTENDS_HANDLE, declB.type);
	assertNull("expected declA.ancestorTypeName = null", declB.ancestorTypeName);
    }

    @Test
    public void testNthDegreeTypeDecls() {

	Chunk parsed = null;
	try {
	    parsed = parse(
		    "type alpha extends handle\r\n"
		    + "type beta extends handle\r\n"
		    + "type delta extends alpha\r\n"
		    + "type epsilon extends beta\r\n"
		    + "type gamma extends delta\r\n"
		    + "type theta extends epsilon");
	} catch (ParseException e) {
	    assertNull("expected no exception", e);
	}

	assertNotNull("expected type declarations", parsed.utypeDecls);
	assertEquals("expected 6 type declarations", 6, parsed.utypeDecls.size());

	/* Check first-degree types, sanity only */
	final TypeDecl typeA = parsed.utypeDecls.get(0),
		typeB = parsed.utypeDecls.get(1);

	assertEquals("expected typeA.name = 'alpha'", "alpha", typeA.typeName);
	assertEquals("expected typeB.name = 'beta'", "beta", typeB.typeName);

	/* Check second-degree types: */
	final TypeDecl typeD = parsed.utypeDecls.get(2),
		typeE = parsed.utypeDecls.get(3);

	/* Check second-degree delta type */
	assertEquals("expected typeD.typeName = 'delta'", "delta", typeD.typeName);
	assertEquals("expected typeD.type = EXTENDS_TYPENAME", TypeDeclType.EXTENDS_TYPENAME, typeD.type);
	assertEquals("expected typeD.ancestorTypeName = 'alpha'", "alpha", typeD.ancestorTypeName);

	/* Check second-degree epsilon type */
	assertEquals("expected typeE.typeName = 'epsilon'", "epsilon", typeE.typeName);
	assertEquals("expected typeE.type = EXTENDS_TYPENAME", TypeDeclType.EXTENDS_TYPENAME, typeE.type);
	assertEquals("expected typeE.ancestorTypeName = 'beta'", "beta", typeE.ancestorTypeName);

	/* Check third-degree types: */
	final TypeDecl typeG = parsed.utypeDecls.get(4),
		typeT = parsed.utypeDecls.get(5);

	/* Check third-degree gamma type */
	assertEquals("expected typeG.typeName = 'gamma'", "gamma", typeG.typeName);
	assertEquals("expected typeG.type = EXTENDS_TYPENAME", TypeDeclType.EXTENDS_TYPENAME, typeG.type);
	assertEquals("expected typeG.ancestorTypeName = 'delta'", "delta", typeG.ancestorTypeName);

	/* Check third-degree theta type */
	assertEquals("expected typeT.typeName = 'theta'", "theta", typeT.typeName);
	assertEquals("expected typeT.type = EXTENDS_TYPENAME", TypeDeclType.EXTENDS_TYPENAME, typeT.type);
	assertEquals("expected typeT.ancestorTypeName = 'epsilon'", "epsilon", typeT.ancestorTypeName);

    }

}
