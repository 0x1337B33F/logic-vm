package logicbyte.bytecode.stream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Lochie
 */
public class NeutralDecoderInputStream {

    protected InputStream srcStreamRef;
    protected DataInputStream srcDataStreamRef;

    public NeutralDecoderInputStream(InputStream is) {
	this.srcStreamRef = is;
	this.srcDataStreamRef = new DataInputStream(is);
    }

    protected String readString() throws IOException {
	StringBuilder sb = new StringBuilder();
	byte b0 = 0x00;
	while ((b0 = srcDataStreamRef.readByte()) != 0x00)
	    sb.append((char) b0);
	return sb.toString();
    }

    protected int readPackedInt() throws IOException {
	long v = readPackedLong();
	if (v > Integer.MAX_VALUE)
	    throw new IOException("integer too large");
	if (v < Integer.MIN_VALUE)
	    throw new IOException("integer too small");
	return (int) (v & 0xffffffff);
    }

    protected long readPackedLong() throws IOException {
	int b0 = srcDataStreamRef.read();
	boolean sign = (b0 & 0b01000000) != 0;
	if ((b0 & 0b10000000) == 0)
	    return (sign) ? (-((long) (b0 & 0b00111111))) - 1 : (long) (b0 & 0b00111111);
	else {
	    long accum = 0;
	    for (int width = (b0 & 0b00111111), i = 0; i < width; i++)
		accum |= ((long) srcDataStreamRef.read() << (i * 8));
	    accum += (long) 0b00111111;
	    return (sign) ? (-accum) - 1 : accum;
	}
    }

}
