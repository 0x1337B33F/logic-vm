package logicscript.ast.nodes.stmt.cfl;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements the LOOP CFL statement: the contained block shall loop until broken by an EXITWHEN CFL statement.
 * </p>
 *
 * @author Lochie
 */
public class LoopStatement extends ControlFlowStatement {

    public Block statements;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (statements != null)
	    v.visitFuncBlock(statements);
    }

}
