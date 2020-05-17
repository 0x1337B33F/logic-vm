package logicbyte.bytecode.stream;

import java.io.IOException;
import java.io.OutputStream;
import logicbyte.bytecode.FunctionOpcodeList;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.bytecode.Opcode;

/**
 *
 * @author Lochie
 */
public class LogicBytecodeOutputStream extends NeutralEncoderOutputStream {

    public LogicBytecodeOutputStream(OutputStream o) {
	super(o);
    }

    public void writeBytecodeFile(LogicBytecodeFile file) throws IOException {
	destDataStreamRef.write(new byte[]{0x13, (byte) 'L', (byte) 'S', (byte) 'f', (byte) 0xff});
	destDataStreamRef.writeShort((short) 1);
	destDataStreamRef.writeShort((short) 0);

	BytecodeFunctionDecl[] loaded = file.getAll();

	if (loaded == null || loaded.length == 0)
	    return;
	else
	    for (int i = 0, j = loaded.length; i < j; i++)
		writeBytecodeFunctionDecl(loaded[i]);
    }

    public void writeBytecodeFunctionDecl(BytecodeFunctionDecl compiledFunction) throws IOException {
	destDataStreamRef.write((byte) compiledFunction.typeflag());
	destDataStreamRef.write((byte) compiledFunction.maskflag());

	writeString(compiledFunction.name());

	if (compiledFunction.hasreturn())
	    writePackedInt(compiledFunction.returntype());

	if (compiledFunction.hasargs()) {
	    writePackedInt(compiledFunction.numargs());
	    for (int i = 0, j = compiledFunction.numargs(); i < j; i++)
		writePackedInt(compiledFunction.argtype(i));

	    int word = 0, count = compiledFunction.numargs();
	    for (int i = 0; i < compiledFunction.numargs(); i += 8) {
		for (int j = 0; j < 8; count--, j++)
		    word = (word << 1) | ((count > 0 && compiledFunction.argarray(i + j)) ? 1 : 0);
		destDataStreamRef.writeByte((byte) word);
	    }
	}

	if (compiledFunction.haslocals()) {
	    writePackedInt(compiledFunction.numlocals());
	    for (int i = 0, j = compiledFunction.numlocals(); i < j; i++)
		writePackedInt(compiledFunction.localtype(i));

	    if (compiledFunction.numlocals() > 0) {
		int word = 0, count = compiledFunction.numlocals();
		for (int i = 0; i < compiledFunction.numlocals(); i += 8) {
		    for (int j = 0; j < 8; count--, j++)
			word = (word << 1) | ((count > 0 && compiledFunction.localarray(i + j)) ? 1 : 0);
		    destDataStreamRef.writeByte((byte) word);
		}
	    }
	}

	if (compiledFunction.isufunc())
	    writeBytecodeFunctionDeclOpList(compiledFunction.opcodes());

	if (compiledFunction.hasdebug())
	    writeBytecodeFunctionDeclDebugSegment(compiledFunction, compiledFunction.debug());

    }

    public void writeBytecodeFunctionDeclDebugSegment(BytecodeFunctionDecl compiledFunction, BytecodeFunctionDecl.DebugSegment debugSegment) throws IOException {
	if (compiledFunction.hasargs()) {
	    writePackedInt(debugSegment.numargnames());
	    for (int i = 0, j = debugSegment.numargnames(); i < j; i++)
		writeString(debugSegment.argname(i));
	}

	if (compiledFunction.isufunc() && compiledFunction.haslocals()) {
	    writePackedInt(debugSegment.numlocalnames());
	    for (int i = 0, j = debugSegment.numlocalnames(); i < j; i++)
		writeString(debugSegment.localname(i));
	}
    }

    public void writeBytecodeFunctionDeclOpList(FunctionOpcodeList opcodes) throws IOException {
	writePackedInt(opcodes.size());
	for (int i = 0, j = opcodes.size(); i < j; i++)
	    writeOpcode(opcodes.get(i));
    }

    public void writeOpcode(Opcode opcodeObj) throws IOException {
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
		    case LONG_LITERAL:
			writePackedLong((Long) opcodeObj.argobject(i));
			break;
		    case STR_LITERAL:
			writeString((String) opcodeObj.argobject(i));
			break;
		}
    }

}
