package logicscript.ast.nodes.stmt.cfl;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.Closure;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements the FOR CFL statement: defines a local closure and assigns the specified local variable name given to the incrementor
 * <code>[0...n]</code> where <code>n</code> is either:</p>
 *
 * <ul>
 * <li>A reference to a local or global variable; (0 to local n iterator) or</li>
 * <li>An expression (0 to EXPR iterator), where EXPR is evaluated <b>once</b></li>
 * </ul>
 *
 * @author Lochie
 */
public class ForStatement extends ControlFlowStatement implements Closure {

    public String local;

    public Expression startExpr;

    public Expression endExpr;

    public Expression byExpr;

    public Block thenBlock;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (startExpr != null)
	    v.visitExpression(startExpr);
	if (endExpr != null)
	    v.visitExpression(endExpr);
	if (byExpr != null)
	    v.visitExpression(byExpr);

	if (thenBlock != null) {
	    v.visitClosure(this);
	    v.visitFuncBlock(thenBlock);
	    v.visitClosureEnd(this);
	}
    }

    @Override
    public boolean hascvars() {
	return (local != null);
    }

    @Override
    public int numcvars() {
	return (hascvars()) ? 1 : 0;
    }

    @Override
    public String getcvarname(int which) {
	if (!hascvars())
	    return null;
	if (which == 0)
	    return local;
	return null;
    }

    @Override
    public TypeDecl getcvartype(int which) {
	if (!hascvars())
	    return null;
	if (which == 0)
	    return TypeDecl.TYPE_INTEGER;
	return null;
    }

}
