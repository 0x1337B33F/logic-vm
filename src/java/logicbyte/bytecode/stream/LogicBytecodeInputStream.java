package logicbyte.bytecode.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.FunctionOpcodeList;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDeclMutable;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.OpcodeControlBitmask;
import logicbyte.bytecode.Opcode.Opcodes;

/**
 *
 * @author Lochie
 */
public class LogicBytecodeInputStream extends NeutralDecoderInputStream {

    final String sourceName;

    public LogicBytecodeInputStream(InputStream i, String sourceName) {
	super(i);
	this.sourceName = sourceName;
    }

    public LogicBytecodeFile readBytecodeFile() throws IOException {
	byte[] header = new byte[ 5 ];
	if (srcDataStreamRef.read(header) != header.length)
	    throw new IOException("Not enough header bytes");

	if (header[0] != (byte) 0x13 || header[1] != (byte) 'L' || header[2] != (byte) 'S' || header[3] != (byte) 'f' || header[4] != (byte) 0xFF)
	    throw new IOException(String.format("Invalid header bytes: %2x %2x %2x %2x %2x", header[0], header[1], header[2], header[3],
		    header[4]));

	short version = srcDataStreamRef.readShort(),
		flags = srcDataStreamRef.readShort();

	if (version != (short) 1 || flags != (short) 0)
	    throw new IOException("Invalid version or flags");

	LogicBytecodeFile allocBytecodeDst = new LogicBytecodeFile(sourceName);
	while (srcDataStreamRef.available() > 0)
	    allocBytecodeDst.add(readBytecodeFunctionDecl());
	return allocBytecodeDst;
    }

    protected BytecodeFunctionDecl readBytecodeFunctionDecl() throws IOException {

	byte typeflag = srcDataStreamRef.readByte(),
		maskflag = srcDataStreamRef.readByte();
	String name = readString();

	FunctionOpcodeList allocFnOpList = null;

	if ((typeflag & BytecodeFunctionDecl.FUNC_TYPE_NATIVE) == 0)
	    allocFnOpList = new FunctionOpcodeList();

	BytecodeFunctionDeclMutable allocBytecodeFnDst = new BytecodeFunctionDeclMutable(typeflag, name, allocFnOpList);

	if ((maskflag & BytecodeFunctionDecl.FUNC_MASK_HASRET) != 0)
	    allocBytecodeFnDst.setreturn(readPackedInt(), (maskflag & BytecodeFunctionDecl.FUNC_MASK_HASRETARR) != 0);

	if ((maskflag & BytecodeFunctionDecl.FUNC_MASK_HASARGS) != 0) {
	    int numargs = readPackedInt();

	    int[] argtable = new int[ numargs ];
	    for (int i = 0, j = argtable.length; i < j; i++)
		argtable[i] = readPackedInt();

	    boolean[] argarrays = new boolean[ numargs ];
	    int words = (int) Math.ceil(numargs / 8.0d);
	    for (int i = 0; i < words; i++) {
		byte word = srcDataStreamRef.readByte();
		for (int j = 0; j < 8; j++) {
		    int k = (i * 8) + j;
		    if (k >= argarrays.length)
			break;
		    argarrays[k] = (word & (1 << (8 - j))) != 0;
		}
	    }

	    allocBytecodeFnDst.setargs(argtable, argarrays);
	}

	if ((maskflag & BytecodeFunctionDecl.FUNC_MASK_HASLOCALS) != 0) {
	    int[] localtypes = new int[ readPackedInt() ];

	    for (int i = 0, j = localtypes.length; i < j; i++)
		localtypes[i] = readPackedInt();

	    boolean[] localarrays = new boolean[ localtypes.length ];
	    int words = (int) Math.ceil(localtypes.length / 8.0d);
	    for (int i = 0; i < words; i++) {
		byte word = srcDataStreamRef.readByte();
		for (int j = 0; j < 8; j++) {
		    int k = (i * 8) + j;
		    if (k >= localtypes.length)
			break;
		    localarrays[k] = (word & (1 << (8 - j))) != 0;
		}
	    }

	    allocBytecodeFnDst.setlocals(localtypes, localarrays);
	}

	if ((typeflag & BytecodeFunctionDecl.FUNC_TYPE_NATIVE) == 0)
	    readBytecodeFunctionDeclOpList(allocFnOpList);

	if ((maskflag & BytecodeFunctionDecl.FUNC_MASK_HASDEBUG) != 0)
	    allocBytecodeFnDst.setdebug(readBytecodeFunctionDeclDebugSegment(allocBytecodeFnDst));

	if ((byte) allocBytecodeFnDst.maskflag() != maskflag)
	    throw new IOException(String.format("Bytecode function reconstitution failure: wanted mask %2x, got mask %2x", maskflag,
		    allocBytecodeFnDst.maskflag()));

	return allocBytecodeFnDst;
    }

    protected BytecodeFunctionDecl.DebugSegment readBytecodeFunctionDeclDebugSegment(BytecodeFunctionDecl fnDecl) throws IOException {

	DebugSegment allocDebugDst = new DebugSegment();

	if (fnDecl.hasargs()) {
	    String[] argnametable = new String[ readPackedInt() ];
	    for (int i = 0, j = argnametable.length; i < j; i++)
		argnametable[i] = readString();
	    allocDebugDst.setargnames(argnametable);
	}

	if (fnDecl.isufunc() && fnDecl.haslocals()) {
	    String[] localnametable = new String[ readPackedInt() ];
	    for (int i = 0, j = localnametable.length; i < j; i++)
		localnametable[i] = readString();
	    allocDebugDst.setlocalnames(localnametable);
	}

	return allocDebugDst;
    }

    protected void readBytecodeFunctionDeclOpList(FunctionOpcodeList dstListObj) throws IOException {
	int numOpcodes = readPackedInt();
	for (int i = 0; i < numOpcodes; i++)
	    dstListObj.add(readOpcode());
	if (dstListObj.size() != numOpcodes)
	    throw new IOException("Bytecode function bytecode reconstitution failure");
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
		    case LONG_LITERAL:
			args.add(readPackedLong());
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
