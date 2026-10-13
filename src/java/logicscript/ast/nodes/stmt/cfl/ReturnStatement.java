package logicscript.ast.nodes.stmt.cfl;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements the RETURN CFL statement: returns a value (if an expression is defined), or nothing, from the current function structure
 * irrespective of the depth of the stack.
 * </p>
 *
 * @author Lochie
 */
public class ReturnStatement extends ControlFlowStatement {

    public Expression returnExpression;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (returnExpression != null)
	    v.visitExpression(returnExpression);
    }

}
