package logicbyte.bytecode.stream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.OpcodeControlBitmask;
import logicbyte.bytecode.Opcode.Opcodes;

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

    protected Opcode readOpcode() throws IOException {
	byte opword = srcDataStreamRef.readByte(), opopts = 0x00;
	Opcode base = Opcodes.lookupBaseOpcode(opword);
	if (base.hasopts())
	    opopts = srcDataStreamRef.readByte();
	if (base.args() > 0) {
	    List<Object> args = new LinkedList<>();
	    for (int i = 0, j = base.args(); i < j; i++)
		switch (base.argtype(i)) {
		    case BOOL_LITERAL:
			args.add(srcDataStreamRef.readByte() != 0x00);
			break;
		    case INT_LITERAL:
			args.add(srcDataStreamRef.readInt());
			break;
		    case INT_PACKED:
			args.add(readPackedInt());
			break;
		    case REAL_LITERAL:
			args.add(srcDataStreamRef.readDouble());
			break;
		    case STR_LITERAL:
			args.add(readString());
			break;
		}
	    try {
		return new OpcodeControlBitmask(base, opopts, args);
	    } catch (InstantiationException e) {
		throw new IOException("Invalid bytecode", e);
	    }
	} else {
	    if (base.hasopts())
		try {
		    return new OpcodeControlBitmask(base, opopts);
		} catch (InstantiationException e) {
		    throw new IOException("Invalid bytecode", e);
		}
	    return base;
	}

    }

}
