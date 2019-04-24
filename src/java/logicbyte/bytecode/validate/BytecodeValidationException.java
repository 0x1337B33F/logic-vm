package logicbyte.bytecode.validate;

import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.bytecode.Opcode;

/**
 *
 * @author Lochie
 */
public class BytecodeValidationException extends Exception {

    public static class IllegalOperandException extends BytecodeValidationException {

	final BytecodeFunctionDecl funcDecl;
	final int funcOpAddr;
	final Opcode funcOp;

	public IllegalOperandException(BytecodeFunctionDecl decl, int addr, Opcode op, String message) {
	    super(message);
	    this.funcDecl = decl;
	    this.funcOpAddr = addr;
	    this.funcOp = op;
	}

	public BytecodeFunctionDecl getFunction() {
	    return this.funcDecl;
	}

	public int getOpcodeAddress() {
	    return this.funcOpAddr;
	}

	public Opcode getOpcode() {
	    return this.funcOp;
	}
    }

    public BytecodeValidationException(String message) {
	super(message);
    }

    public BytecodeValidationException(String message, Throwable inner) {
	super(message, inner);
    }
}
