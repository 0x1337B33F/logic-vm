package logicbyte.bytecode;

import java.util.LinkedList;
import logicbyte.bytecode.Opcode.Opcodes;

/**
 *
 * @author Lochie
 */
public class FunctionOpcodeList extends LinkedList< Opcode> {

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(this.getClass().getName()).append("{");
	sb.append("numopcodes=").append(this.size()).append(",");
	sb.append("opcodes=[");

	for (int i = 0, j = this.size(); i < j; i++) {
	    Opcode o = this.get(i);
	    sb.append("\r\n\t").append(i).append(": ");
	    sb.append(o.getword()).append(" (").append( Opcodes.lookupBaseOpcode( o.getword() ) ).append(")");
	    if (o.hasopts())
		sb.append("/").append(o.getopts());
	    
	    if (o.args() > 0) {
		sb.append(" [");
		for (int k = 0, l = o.args(); k < l; k++) {
		    sb.append("(").append(o.argtype(k)).append(") ").append(o.argobject(k));
		    if (k < (l - 1))
			sb.append(",");
		}
		sb.append("]");
	    }
	}
	return sb.append("}").toString();
    }

}
