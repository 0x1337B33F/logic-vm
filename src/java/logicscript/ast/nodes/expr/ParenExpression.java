package logicscript.ast.nodes.expr;

import logicscript.ast.ChunkVisitor;

/**
 * <p>
 * Implementation of an expression node which contains only a nested expression node; used only to parse fully the original structure of the
 * expression by the lexer. It may not be necessary to include parenthesis at certain location(s).</p>
 *
 * @author Lochie
 */
public class ParenExpression extends Expression {

    public Expression expressionInParenth;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	v.visitExpression(expressionInParenth);
    }

}
