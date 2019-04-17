package logicscript.ast.nodes.expr;

import logicscript.ast.ChunkVisitor;

/**
 * <p>
 * Implementation of a variable array index reference expression; the return type of the expression becomes the type of the local variable,
 * global variable or imported global variable. The value of the variable may not be specifically consistent with the initialization
 * expression of the variable; the real value of the variable shall not be known until execution.</p>
 *
 * <p>
 * The value of the index expression shall always resolve to an <code>integer</code>, else an exception shall be raised. The value of the
 * index expression must be within the bounds <code>[0, }</code>. The local, global or imported global must exist in the appropriate
 * scope.</p>
 *
 * @author Lochie
 */
public class VariableArrayRefExpression extends Expression {

    public String nameOfLocalOrGlobalVar;
    public Expression indexOfArrayExpr;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (indexOfArrayExpr != null)
	    v.visitExpression(indexOfArrayExpr);
    }

}
