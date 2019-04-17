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
	byte b0 = srcDataStreamRef.readByte();
	if ((b0 & 0b11000000) == 0)
	    return (long) b0;
	else {
	    long accum = 0b00111111;
	    if ((b0 & 0b01000000) == 0b01000000)
		return (long) (accum + (b0 & 0b00111111));
	    else if ((b0 & 0b10000000) == 0b10000000) {
		accum += srcDataStreamRef.readByte();
		accum += (srcDataStreamRef.readByte() << 8);
		return accum;
	    } else {
		accum += 0x0000ffff;
		if ((b0 & 0b11111100) != 0b11000000)
		    throw new IOException("number format decode failure");

		if ((b0 & 0b00000011) == 0)
		    throw new IOException("number format decode failure");

		if ((b0 & 0b00000010) == 0b00000010)
		    for (int i = 0; i < 8; i++)
			accum += (srcDataStreamRef.readByte() << (i * 8));
		else
		    for (int i = 0; i < 4; i++)
			accum += (srcDataStreamRef.readByte() << (i * 8));
		return accum;
	    }
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
			args.add((srcDataStreamRef.readByte() != 0x00) ? true : false);
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
