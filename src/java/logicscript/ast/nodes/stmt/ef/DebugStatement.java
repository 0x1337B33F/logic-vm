package logicscript.ast.nodes.stmt.ef;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.nodes.stmt.ExecutionFunctionStatement;
import logicscript.ast.nodes.stmt.Statement;

/**
 * <p>
 * Implements the DEBUG statement: the nested statement (and all entering calls as a result of the next statement) shall be performed with
 * debugging on. If the statement is a control- flow statement, the entire control-flow statement shall be debugged.</p>
 *
 * <p>
 * The state of DEBUG is immutable: subsequent calls to DEBUG when already in DEBUG do not disable debug.</p>
 *
 * @author Lochie
 */
public class DebugStatement extends ExecutionFunctionStatement {

    public Statement statement;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (statement != null)
	    v.visitStatement(statement);
    }

}
