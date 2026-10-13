package logicscript.ast;

/**
 * <p>
 * Contract interface for AST object(s) which are visitable; AST objects which are visible contain sub-nodes or descendants which must be
 * revealed when the node itself is visited, recursively.</p>
 *
 * <p>
 * Implementors of the Visitable interface must ensure that they invoke the appropriate <code>visitXXX</code> method on the visiting
 * ChunkVisitor object.</p>
 *
 * @author Lochie
 */
public interface Visitable {

    public void visit(ChunkVisitor v) throws Exception;

}
