package logicscript.ast.nodes.stmt.ef;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.ExecutionFunctionStatement;

/**
 *
 * <p>
 * Implements the SETVARARR statement: sets a variable (local or global) by name at index left-hand-side expression to the right-hand-side
 * expression.</p>
 *
 * @author Lochie
 */
public class SetArrayVariableStatement extends ExecutionFunctionStatement {

    public String nameOfLocalVar;

    public Expression indexOfArrayExpr;

    public Expression rhs;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (indexOfArrayExpr != null)
	    v.visitExpression(indexOfArrayExpr);
	if (rhs != null)
	    v.visitExpression(rhs);
    }

}
