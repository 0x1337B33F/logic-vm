package logicscript.ast.nodes.stmt.cfl;

import java.util.Iterator;
import java.util.List;
import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements IF CFL statement: contains at least the first conditional expression and the proceeding block, then:
 * </p>
 *
 * <ul>
 * <li>Any number of ELSEIF blocks; expressed as elseIfCondExprs &amp; elseIfBlocks;</li>
 * <li>A final optional ELSE block.</li>
 * </ul>
 *
 *
 * @author Lochie
 */
public class IfStatement extends ControlFlowStatement {

    public Expression conditionalExpr;
    public Block thenBlock;
    public Block elseBlock;

    public List<Expression> elseIfCondExprs;
    public List<Block> elseIfBlocks;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (conditionalExpr != null)
	    v.visitExpression(conditionalExpr);
	if (thenBlock != null)
	    v.visitFuncBlock(thenBlock);
	if (elseIfCondExprs != null && elseIfBlocks != null)
	    synchronized ( elseIfCondExprs ) {
		synchronized ( elseIfBlocks ) {
		    Iterator<Expression> itx0 = elseIfCondExprs.iterator();
		    Iterator<Block> itx1 = elseIfBlocks.iterator();
		    while (itx0.hasNext() && itx1.hasNext()) {
			v.visitExpression(itx0.next());
			v.visitFuncBlock(itx1.next());
		    }
		}
	    }

	if (elseBlock != null)
	    v.visitFuncBlock(elseBlock);
    }

}
