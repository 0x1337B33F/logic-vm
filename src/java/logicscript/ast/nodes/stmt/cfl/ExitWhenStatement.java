package logicscript.ast.nodes.stmt.cfl;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements the EXITWHEN CFL statement: used only within LOOP CFL statement(s) to break the current loop if the right-hand expression
 * resolves to TRUE.
 * </p>
 *
 * @author Lochie
 */
public class ExitWhenStatement extends ControlFlowStatement {

    public Expression condExitWhenExpr;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (condExitWhenExpr != null)
	    v.visitExpression(condExitWhenExpr);
    }

}
