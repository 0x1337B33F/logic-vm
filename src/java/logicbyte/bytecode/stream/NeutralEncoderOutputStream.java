package logicbyte.bytecode.stream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
		destDataStreamRef.write((byte) cbuf[i]);
	}
	destDataStreamRef.write((byte) 0x00);
    }

    protected void writePackedInt(long i) throws IOException {
	writePackedLong(i);
    }

    protected void writePackedLong(long l) throws IOException {
	long v = (0L > l) ? Math.abs(l + 1) : Math.abs(l);
	byte sign = (byte) ((v != l) ? 0b01000000 : 0b00000000);
	if (v < 0b00111111)
	    destDataStreamRef.write((byte) ((v & 0b00111111) | sign));
	else {
	    v -= 0b00111111;
	    int width = 0;
	    for (long v1 = v; v1 > 0; v1 >>= 8)
		width++;
	    if (width > 0b00111111)
		throw new IOException("number too wide");
	    destDataStreamRef.write((byte) (0b10000000 | sign | width));
	    for (int i = 0; i < width; v >>= 8, i++)
		destDataStreamRef.write((byte) (v & (long) 0x00000000000000ff));
	}
    }
}
