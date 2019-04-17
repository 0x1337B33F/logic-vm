package logicscript.ast.nodes.stmt.cfl;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements the RAISE CFL statement: raises an exception up the stack to the next TRYCATCH CFL statement, or
 * to the Java exception trap.</p>
 * 
 * <p>The Expression may only resolve to one of the following types:</p>
 * <ul>
 * <li><code>string</code> type;</li>
 * <li><code>integer</code> type;</li>
 * <li><code>real</code> type;</li>
 * <li><code>booleam</code> type.</li>
 * </ul>
 * 
 * <p>Attempting to throw a value of a complex type shall result in nothing being thrown instead. While this is
 * the only way to throw nothing, do not do so; throwing nothing indicates the RAISE exception is invalid also,
 * and should be checked!</p>
 * 
 * @author Lochie
 */
public class RaiseStatement extends ControlFlowStatement {

	public Expression raiseValue;

	@Override
	public void visit(ChunkVisitor v) throws Exception {
		if (raiseValue != null)
			v.visitExpression(raiseValue);
	}

}
