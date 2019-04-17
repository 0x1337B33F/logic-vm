package logicscript.ast.nodes.expr;

import logicbyte.bytecode.OpcodeParamValueTable.UnaryExpressionParamValueTable;
import logicscript.ast.ChunkVisitor;

/**
 * <p>
 * Implementation of a unary (one-) value operation; the operation requires a right-hand-side nested expression.</p>
 *
 * @author Lochie
 */
public class UnaryExpression extends Expression {

    public static enum UnaryOperation {
	NUM_NEGATE, NUM_ABSVAL, BIT_NEGATE, BOOL_NOT;

	/**
	 * Forward lookup to the assembled parameter value instance; this is such that the AST need not be
	 * carried around all the time with garbage attached.
	 * @return 
	 */
	public UnaryExpressionParamValueTable toAssemblyType() {
	    return UnaryExpressionParamValueTable.valueOf(this.name());
	}

    }

    public UnaryOperation operand;
    public Expression rhs;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	v.visitExpression(rhs);

    }

}
