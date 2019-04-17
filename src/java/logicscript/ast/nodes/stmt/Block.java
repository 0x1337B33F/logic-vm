package logicscript.ast.nodes.stmt;

import java.util.Iterator;
import java.util.LinkedList;
import logicscript.ast.ChunkVisitor;
import logicscript.ast.Visitable;

/**
 * <p>
 * Implements a collection of Statement(s). A Block may occur as the top-level object of a function declaration, or as part of any
 * control-flow statement (as a nested block).
 * </p>
 *
 * @author Lochie
 */
public class Block extends LinkedList<Statement> implements Visitable {

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	synchronized ( this ) {
	    Iterator<Statement> itx = this.iterator();
	    while (itx.hasNext())
		v.visitStatement(itx.next());
	}
    }
}
