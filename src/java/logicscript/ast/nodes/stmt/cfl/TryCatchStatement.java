package logicscript.ast.nodes.stmt.cfl;

import java.util.Iterator;
import java.util.List;
import logicscript.ast.ChunkVisitor;
import logicscript.ast.Closure;
import logicscript.ast.Location;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.ControlFlowStatement;

/**
 * <p>
 * Implements the TRYCATCH CFL statement: the block in the TRY section shall be executed such that any exception (such as one thrown by
 * RAISE, or by some other error) shall result in the subsequent CATCH closure being executed.
 * </p>
 *
 * @author Lochie
 */
public class TryCatchStatement extends ControlFlowStatement {

    public static class CatchBlock implements Closure {

	public TypeDecl typeOfCatch;
	
	public Location location;

	public String catchLocal;

	public Block block;

	@Override
	public boolean hascvars() {
	    return typeOfCatch != null && typeOfCatch != TypeDecl.TYPE_NOTHING && catchLocal != null;
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
		return catchLocal;
	    return null;
	}

	@Override
	public TypeDecl getcvartype(int which) {
	    if (!hascvars())
		return null;
	    if (which == 0)
		return typeOfCatch;
	    return null;
	}
    }

    /**
     * The block to TRY execute
     */
    public Block tryBlock;

    public List<CatchBlock> catchBlocks;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (tryBlock != null)
	    v.visitFuncBlock(tryBlock);

	if (catchBlocks != null) {
	    Iterator<CatchBlock> itx = catchBlocks.iterator();
	    while (itx.hasNext()) {
		CatchBlock catchBlock = itx.next();
		v.visitClosure((Closure) catchBlock);
		v.visitFuncBlock(catchBlock.block);
		v.visitClosureEnd((Closure) catchBlock);
	    }
	}

    }

}
