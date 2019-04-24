package logicscript.ast.nodes.stmt.cfl;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements the YIELD CFL statement; yields a value (if an expression is defined), or nothing, form the current function.
 * </p>
 *
 * @author Lochie
 */
public class YieldStatement extends ControlFlowStatement {
    
    public Expression yieldExpression;
    
    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (yieldExpression != null)	    
	    v.visitExpression(yieldExpression);
    }
    
}
