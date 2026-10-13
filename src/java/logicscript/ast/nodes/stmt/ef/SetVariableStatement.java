package logicscript.ast.nodes.stmt.ef;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.ExecutionFunctionStatement;

/**
 * <p>
 * Implements the SETVAR statement: sets a variable (local or global) by name to the right-hand-side expression.</p>
 *
 * @author Lochie
 */
public class SetVariableStatement extends ExecutionFunctionStatement {

    public String nameOfLocalVar;

    public Expression rhs;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (rhs != null)
	    v.visitExpression(rhs);
    }

}
