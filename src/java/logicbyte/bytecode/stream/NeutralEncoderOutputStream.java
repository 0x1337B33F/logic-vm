package logicbyte.bytecode.stream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import logicbyte.bytecode.Opcode;

/**
 *
 * @author Lochie
 */
public class NeutralEncoderOutputStream {

    protected OutputStream destStreamRef;
    protected DataOutputStream destDataStreamRef;

    public NeutralEncoderOutputStream(OutputStream os) {
	this.destStreamRef = os;
	this.destDataStreamRef = new DataOutputStream(os);
    }

    public void closeWrapper() throws IOException {
	if (this.destDataStreamRef == null)
	    return;
	this.destDataStreamRef.flush();
	this.destDataStreamRef.close();
	this.destDataStreamRef = null;
    }

    protected void writeString(String s) throws IOException {
	if (s != null && s.length() != 0) {
	    char[] cbuf = s.toCharArray();
	    for (int i = 0, j = cbuf.length; i < j; i++)
		destDataStreamRef.write ( ( byte) cbuf[i]);
	}
	destDataStreamRef.write((byte) 0x00);
    }

    protected void writePackedInt(long v) throws IOException {
	if (v < 0b00111111)	// word fits as a single byte:
	    destDataStreamRef.write((byte) (v & 0b00111111));
	else {
	    v -= 0b00111111;
	    if (v < 0b00111111)  // word fits as a byte after next:
		destDataStreamRef.write((byte) (0b01000000 | (v & 0b00111111)));
	    else if (v < 0x0000ffff) { // word fits as a short:
		destDataStreamRef.write((byte) 0b10000000);
		destDataStreamRef.write((byte) (v & 0x000000ff));
		destDataStreamRef.write((byte) ((v & 0x0000ff00) >> 8));
	    } else {
		v -= 0x0000ffff;
		if (v < 0xffffffff) {
		    destDataStreamRef.write((byte) 0b11000001);
		    destDataStreamRef.write((byte) (v & 0x000000ff));
		    destDataStreamRef.write((byte) ((v & 0x0000ff00) >> 8));
		    destDataStreamRef.write((byte) ((v & 0x00ff0000) >> 16));
		    destDataStreamRef.write((byte) ((v & 0xff000000) >> 24));
		} else {
		    destDataStreamRef.write((byte) 0b11000010);
		    long v1 = v;
		    for (int i = 0; i < 8; i++) {
			destDataStreamRef.write((byte) (v1 & 0x00000000000000ff));
			v1 = v1 >> 8;
		    }
		}
	    }
	}
    }

    protected void writePackedLong(long l) throws IOException {
	writePackedInt(l);
    }

    protected void writeOpcode(Opcode opcodeObj) throws IOException {
	destDataStreamRef.write(opcodeObj.getword());
	if (opcodeObj.hasopts())
	    destDataStreamRef.write(opcodeObj.getopts());
	if (opcodeObj.args() > 0)
	    for (int i = 0, j = opcodeObj.args(); i < j; i++)
		switch (opcodeObj.argtype(i)) {
		    case BOOL_LITERAL:
			destDataStreamRef.write(((Boolean) opcodeObj.argobject(i) == true) ? 0xff : 0x00);
			break;
		    case INT_LITERAL:
			destDataStreamRef.writeInt((Integer) opcodeObj.argobject(i));
			break;
		    case INT_PACKED:
			writePackedInt((Integer) opcodeObj.argobject(i));
			break;
		    case REAL_LITERAL:
			destDataStreamRef.writeDouble((Double) opcodeObj.argobject(i));
			break;
		    case STR_LITERAL:
			writeString((String) opcodeObj.argobject(i));
			break;
		}
    }
}
