package logicscript.ast.nodes.expr;

import java.util.Iterator;
import java.util.List;
import logicscript.ast.ChunkVisitor;

/**
 * <p>
 * Implementation of a function call expression node; the return type of the function becomes the type of the expression node whose value is
 * yet unknown. The function call expression node may declare zero or more expression parameter(s) which shall be passed to the referent
 * function.</p>
 *
 * <p>
 * If an exception is RAISEd from within the called function, the thrown shall be propagated up the stack.</p>
 *
 * @author Lochie
 */
public class FunctionCallExpression extends Expression {

    public String nameOfFunction;

    public List<Expression> params;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (params != null) {
	    Iterator<Expression> itx = params.iterator();
	    while (itx.hasNext())
		v.visitExpression(itx.next());
	}
    }

}
