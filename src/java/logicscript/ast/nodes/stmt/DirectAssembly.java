package logicscript.ast.nodes.stmt;

import java.util.List;
import logicbyte.bytecode.Opcode;
import logicscript.ast.ChunkVisitor;

/**
 * Statement node which is the equivalent of the <code>asm { }</code> C block.
 *
 * @author Lochie
 */
public class DirectAssembly extends Statement {

    public List<Opcode> opcodes;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
    }

}
