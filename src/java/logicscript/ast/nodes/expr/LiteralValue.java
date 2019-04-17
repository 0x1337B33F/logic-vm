package logicscript.ast.nodes.expr;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.decl.TypeDecl;

/**
 * <p>
 * Implementation of a literal value expression node; the node contains:
 * </p>
 *
 * <ul>
 * <li>A TypeDecl reference indicating the type of the value; and</li>
 * <li>An Object pointer to the Object (boxed primitive) which contains the actual parsed value</li>
 * </ul>
 *
 *
 *
 * @author Lochie
 */
public class LiteralValue extends Expression {

    /**
     * The value used to express boolean TRUE, const:
     */
    public static final LiteralValue VALUE_TRUE = new LiteralValue(TypeDecl.TYPE_BOOLEAN, true);

    /**
     * The value used to express boolean FALSE, const:
     */
    public static final LiteralValue VALUE_FALSE = new LiteralValue(TypeDecl.TYPE_BOOLEAN, false);

    public TypeDecl typeOfValue;

    public Object valueObject;

    public LiteralValue() {
    }

    public LiteralValue(TypeDecl t, Object v) {
	this.typeOfValue = t;
	this.valueObject = v;
    }

    @Override
    public void visit(ChunkVisitor v) {
    }

}
