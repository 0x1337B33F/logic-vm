package logicscript.ast.nodes.expr;

import logicscript.ast.ChunkVisitor;

/**
 * <p>
 * Implementation of a user-function pointer (type `<code>code</code>`) expression node; the return type of the expression shall be
 * <code>code</code> if the function exists, else it shall be <code>nothing</code>.</p>
 *
 * @author Lochie
 */
public class FunctionRefExpression extends Expression {

    public String nameOfFunction;

    @Override
    public void visit(ChunkVisitor v) {
    }

}
