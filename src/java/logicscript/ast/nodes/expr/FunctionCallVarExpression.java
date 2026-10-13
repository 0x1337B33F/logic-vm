package logicscript.ast.nodes.expr;

import java.util.Iterator;
import java.util.List;
import logicscript.ast.ChunkVisitor;

/**
 * <p>
 * Implementation of a user-function pointer (type `<code>code</code>`) call expression; the return type and value of the function is yet
 * unknown. The user-function pointer call expression may declare zero or more expression parameter(s) which shall be passed to the referent
 * function.
 * </p>
 *
 *
 * <p>
 * If, at execution, the return type of the function is incompatible with the expression, an exception will be raised. If an exception is
 * RAISEd within the called function, the thrown shall be propagated up the stack.</p>
 *
 * @author Lochie
 */
public class FunctionCallVarExpression extends Expression {

    public String nameOfLocalOrGlobalVar;

    public List<Expression> params;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	Iterator<Expression> itx = params.iterator();
	while (itx.hasNext())
	    v.visitExpression(itx.next());
    }
}
