package logicscript.ast.nodes.stmt.ef;

import java.util.Iterator;
import java.util.List;
import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.ExecutionFunctionStatement;

/**
 * <p>
 * Implements the FUNCCALLVAR statement: calls a user-function pointer (type `<code>code</code>`) referenced by the local or global variable
 * specified, ignoring any return result; zero or more parameters (as Expression(s)) may be provided.</p>
 *
 * @author Lochie
 */
public class FunctionCallVarStatement extends ExecutionFunctionStatement {

    public String nameOfLocalOrGlobalVar;

    public List<Expression> params;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (params != null)
	    synchronized ( params ) {
		Iterator<Expression> itx = params.iterator();
		while (itx.hasNext())
		    v.visitExpression(itx.next());
	    }
    }
}
