package logicbyte.bytecode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * The opcode contract interface, which is any representation of an operand, such as:
 *
 * <ul>
 * <li>A constant, no arguments value declaration of the opcode which is used to store once the global properties of the opcode; or</li>
 * <li>A value box declaration (such as that loaded from a file) which contains a bitmask or argument values (so long as the referent
 * no-arguments value declaration of the opcode supports such value(s)).</li>
 * </ul>
 *
 * It is expected that all known operands shall exist within the Opcodes set.
 *
 *
 * @author Lochie
 */
public interface Opcode {

    public Opcodes opcode();

    /**
     * Get if the opcode supports options (ie, has OPTION mode mask bits).
     *
     * @return If the opcode has OPTION bit(s)
     */
    public boolean hasopts();

    /**
     * Get the byte value of the OPTIONs bits. If the opcode does not support options (ie, it is a basic opcode and does not require
     * value-boxing), throws an IllegalStateException.
     *
     * @return The value of the OPTIONs bits.
     */
    public byte getopts();

    /**
     * Get whether an option bit is set in the OPTION mask. If the opcode does not support options (ie, it is a basic opcode and does not
     * require value-boxing), throws an IllegalStateException.
     *
     * @param mask The mask bit(s) to check
     *
     * @return If any of the mask bit(s) defined in the parameter is set in the OPTIONs
     */
    public boolean getopt(byte mask);

    /**
     * Get the byte used to represent this opcode within the VM
     *
     * @return The byte used to represent this opcode
     */
    public byte getword();

    /**
     * Get if the opcode has arguments following the opcode (and OPTIONs, if supported).
     *
     * @return The number of arguments following the opcode, or <code>-1</code> if no arguments.
     */
    public int args();

    /**
     * Get the data type of the argument value which shall follow the opcode (and OPTIONs, if supported).
     *
     * @param slot The slot number
     *
     * @return The data type of the argument value
     */
    public OpcodeArgType argtype(int slot);

    /**
     * Get the value assigned to the argument slot for the opcode. If the opcode is not a specific value box instance (ie, is the generic
     * global specification of the opcode and not a compiled opcode), an IllegalStateException shall be thrown.
     *
     * @param slot
     *
     * @return
     */
    public Object argobject(int slot);

    /**
     * Expression of the type of an argument value which follows an operand....
     */
    public static enum OpcodeArgType {
	/**
	 * a packed integer number
	 */
	INT_PACKED(-1),
	/**
	 * boolean literal, one bit
	 */
	BOOL_LITERAL(1),
	/**
	 * integer literal, thirty two bits
	 */
	INT_LITERAL(32),
	/**
	 * real literal, double precision, sixty four bits
	 */
	REAL_LITERAL(64),
	/**
	 * string literal, null terminated
	 */
	STR_LITERAL(-1);

	/**
	 * in lieu of compiler directives...:
	 *
	 * @param varargs the variadic args list
	 *
	 * @return varargs as list of object
	 */
	public static List<OpcodeArgType> listOf(OpcodeArgType... varargs) {
	    List<OpcodeArgType> alloc = new LinkedList<>();
	    for (OpcodeArgType opcode : varargs)
		alloc.add(opcode);
	    return alloc;
	}

	/**
	 * the bitwidth of the argument type, if known and fixed
	 */
	final int numberOfBitsWidth;

	OpcodeArgType(int w) {
	    this.numberOfBitsWidth = w;
	}

	boolean isFixedWidth() {
	    return this.numberOfBitsWidth > 0;
	}

	int getFixedBitWidth() {
	    return this.numberOfBitsWidth;
	}
    }

    /**
     * The base operand set
     */
    public static enum Opcodes implements Opcode {

	/**
	 * Do nothing
	 */
	NOP(0x00, false, null),
	/**
	 * JMP: jump (conditionally or unconditionally) to a relative or absolute offset in current function or closure
	 */
	JMP(0x01, true, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * DUP: duplicate top value on stack
	 */
	DUP(0x02, false, null),
	/**
	 * PUSHB: push boolean on stack
	 */
	PUSHB(0x03, false, OpcodeArgType.listOf(OpcodeArgType.BOOL_LITERAL)),
	/**
	 * PUSHI: push integer on stack
	 */
	PUSHI(0x04, false, OpcodeArgType.listOf(OpcodeArgType.INT_LITERAL)),
	/**
	 * PUSHR: push real on stack
	 */
	PUSHR(0x05, false, OpcodeArgType.listOf(OpcodeArgType.REAL_LITERAL)),
	/**
	 * PUSHS: push string on stack
	 */
	PUSHS(0x06, false, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL)),
	/**
	 * POP: discard top of stack
	 */
	POP(0x07, false, null),
	/**
	 * FUNCPTR: generate function pointer to function name
	 */
	FUNCPTR(0x08, false, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL)),
	/**
	 * RETN: return (flag indicates value / no value ) from function
	 */
	RETN(0x09, true, null),
	/**
	 * CALL: call function by name, num args (flag indicates mandated return yes/no)
	 */
	CALL(0x0A, true, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL, OpcodeArgType.INT_PACKED)),
	/**
	 * CALLLOCAL: call function by local var user pointer, num args (flag indicates mandated return yes/no)
	 */
	CALLLOCAL(0x0B, true, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED, OpcodeArgType.INT_PACKED)),
	/**
	 * CALLGLOBAL: call function by global var user pointer, num args (flag indicates mandated return yes/no)
	 */
	CALLGLOBAL(0x0C, true, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL, OpcodeArgType.INT_PACKED)),
	/**
	 * BINOP: do binary operation on stack (a {
	 *
	 * @ stack 0}, b {
	 * @ stack 1}) -> to stack
	 */
	BINOP(0x0D, true, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * UNOP: do binary operation on stack (a {
	 *
	 * @ stack 0}) -> to stack
	 */
	UNOP(0x0E, true, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * INCLOSURE: enter closure section
	 */
	INCLOSURE(0x0F, false, null),
	/**
	 * ENDCLOSURE: end closure section
	 */
	ENDCLOSURE(0x10, false, null),
	/**
	 * NEWUPVAR: create new lvar in closure (must be INCLOSURE)
	 */
	NEWUPVAR(0x11, true, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED, OpcodeArgType.INT_PACKED)),
	/**
	 * NEWARRAY: allocate new array and assign to lvar in function scope
	 */
	NEWARRAY(0x12, false, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED, OpcodeArgType.INT_PACKED)),
	/**
	 * SETLOCAL: set local by local number (local in closure -> local in func)
	 */
	SETLOCAL(0x13, false, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * SETGLOBAL: set global by var name
	 */
	SETGLOBAL(0x14, false, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL)),
	/**
	 * SETLOCALARR: set local array index by local number, index value on top of stack (consumed) to value on stack after consume
	 */
	SETLOCALARR(0x15, false, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * SETGLOBALARR: set global array index by var name, index value on top of stack (consumed) to value on stack after consume
	 */
	SETGLOBALARR(0x16, false, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL)),
	/**
	 * GETLOCAL: get value of local var by local number, put on stack
	 */
	GETLOCAL(0x17, false, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * GETGLOBAL: get global var by var name, put on stack
	 */
	GETGLOBAL(0x18, false, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL)),
	/**
	 * GETLOCALARR: get value of local var array by local number, index on top of stack -> value put on stack
	 */
	GETLOCALARR(0x19, false, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * GETGLOBALARR: get value of global var array by var name, index on top of stack -> value put on stack
	 */
	GETGLOBALARR(0x1A, false, OpcodeArgType.listOf(OpcodeArgType.STR_LITERAL)),
	/**
	 * DEBUG: do debug on next opcode, if opcode is CALL or some decent then inherit DEBUG downwardly
	 */
	DEBUG(0x1B, false, null),
	/**
	 * TRY: execute all following statements until CATCH as TRY
	 */
	TRY(0x1C, false, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED)),
	/**
	 * CATCH: first or nth CATCH block of a specified type (indicates if m_lastNode in bitmask)
	 */
	CATCH(0x1D, true, OpcodeArgType.listOf(OpcodeArgType.INT_PACKED, OpcodeArgType.INT_PACKED)),
	/**
	 * RAISE: raise exception, whatever is on the top of the stack (may not be nothing)
	 */
	RAISE(0x1E, false, null),
	/**
	 * CMP: compare against boolean true, store in CMP internal register
	 */
	CMP(0x1F, true, null),
	/**
	 * BREAK: causes the current VM to stop executing instructions temporarily if a debugger attached, otherwise, wastes one cycle
	 */
	BREAK(0x20, false, null);

	/*
	 * we expect to be mostly doing forward lookups: don't expect the order of values() to be
	 * consistent with the operand word (even though according to specification values() should
	 * return the collection in the defined enum order).
	 */
	private static HashMap<Integer, Opcodes> opcodeLookupTable;

	public static Opcodes lookupBaseOpcode(byte opword) {
	    return opcodeLookupTable.get((int) opword);
	}

	public static void introduceOpcodeImpl(Opcodes theImplRef) {
	    // ... but it's a compiler check, so just move it somewhere else
	    // and it's really none the wiser.... :->
	    if (Opcodes.opcodeLookupTable == null)
		Opcodes.opcodeLookupTable = new HashMap<>();
	    Opcodes.opcodeLookupTable.put((int) theImplRef.typeword, theImplRef);
	}

	public static final byte BITMASK_BIT_0 = 0b00000001;
	public static final byte BITMASK_BIT_1 = 0b00000010;
	public static final byte BITMASK_BIT_2 = 0b00000100;
	public static final byte BITMASK_BIT_3 = 0b00001000;

	/**
	 * Whether the JMP should use the value on the stack
	 */
	public static final byte BITMASK_JMP_JMPS = BITMASK_BIT_0;
	/**
	 * Whether the value will be 0=relative or 1=absolute addr
	 */
	public static final byte BITMASK_JMP_AORO = BITMASK_BIT_1;
	/**
	 * Whether the JMP should check CMP first, JMP if CMP = true
	 */
	public static final byte BITMASK_JMP_JCJT = BITMASK_BIT_2;
	/**
	 * Whether the JMP should check CMP first, JMP if CMP = false
	 */
	public static final byte BITMASK_JMP_JCJF = BITMASK_BIT_3;

	/**
	 * Mask used to checkbits() on JMPs
	 */
	public static final byte BITMASK_JMP = BITMASK_JMP_JMPS | BITMASK_JMP_AORO | BITMASK_JMP_JCJT | BITMASK_JMP_JCJF;

	/**
	 * Whether the RETN will take top stack value and return it to the callee if the callee desires the value, else it emits nothing
	 */
	public static final byte BITMASK_RETN_RVAL = BITMASK_BIT_0;

	/**
	 * Mask used to checkbits() on RETNs
	 */
	public static final byte BITMASK_RETN = BITMASK_RETN_RVAL;

	/**
	 * Whether the CALL will take the return value from the callee and place it on the top of the stack or not; if the opcode expects a
	 * value and one is not obtained, throws exception
	 */
	public static final byte BITMASK_CALL_RVAL = BITMASK_BIT_0;

	/**
	 * Mask used to checkbits() on CALLs
	 */
	public static final byte BITMASK_CALL = BITMASK_CALL_RVAL;

	/**
	 * Whether the CALLLOCAL will take the return value from the callee and place it on the top of the stack or not; if the opcode
	 * expects a value and one is not obtained, throws exception
	 */
	public static final byte BITMASK_CALLLOCAL_RVAL = BITMASK_BIT_0;

	/**
	 * Mask used to checkbits() on CALLLOCALs
	 */
	public static final byte BITMASK_CALLLOCAL = BITMASK_CALLLOCAL_RVAL;

	/**
	 * Whether the CALLGLOBAL will take the return value from the callee and place it on the top of the stack or not; if the opcode
	 * expects a value and one is not obtained, throws exception
	 */
	public static final byte BITMASK_CALLGLOBAL_RVAL = BITMASK_BIT_0;

	/**
	 * Mask used to checkbits() on CALLGLOBALs
	 */
	public static final byte BITMASK_CALLGLOBAL = BITMASK_CALLGLOBAL_RVAL;

	/**
	 * Whether the NEWUPVAR creates an array in the slot specified; if set, the size of the array is on the top of the stack
	 */
	public static final byte BITMASK_NEWUPVAR_ARRAY = BITMASK_BIT_0;

	/**
	 * Mask used to checkbits() on NEWUPVARs
	 */
	public static final byte BITMASK_NEWUPVAR = BITMASK_NEWUPVAR_ARRAY;

	/**
	 * Whether the current CATCH segment has another CATCH segment following it or not
	 */
	public static final byte BITMASK_CATCH_HASNEXT = BITMASK_BIT_0;

	/**
	 * Mask used to checkbits() on CATCHes
	 */
	public static final byte BITMASK_CATCH = BITMASK_CATCH_HASNEXT;

	/**
	 * Whether the current COMPARE statement will consume the stack value or not
	 */
	public static final byte BITMASK_CMP_CONSUME = BITMASK_BIT_0;

	/**
	 * Mask used to checkbits() on CMPs
	 */
	public static final byte BITMASK_CMP = BITMASK_CMP_CONSUME;

	/**
	 * The system byte used to represent the opcode
	 */
	final byte typeword;
	/**
	 * If the opcode has a set of mask bit(s) following
	 */
	final boolean hasMask;
	/**
	 * If the opcode has argument(s), what the data type shall be; or null
	 */
	final List<OpcodeArgType> args;

	Opcodes(int typeword, boolean mask, List<OpcodeArgType> args) {
	    this.typeword = (byte) typeword;
	    this.hasMask = mask;
	    this.args = args;
	    /*
	     * JVM dislikes modification of things which should have been initialized by clinit only,
	     * especially if you use 'this' references as well -- since it results in nasty behavior
	     * usually::
	     */
	    Opcodes.introduceOpcodeImpl(this);
	}

	@Override
	public Opcodes opcode() {
	    return this;
	}

	public boolean hasopts() {
	    return hasMask;
	}

	public byte getopts() {
	    throw new IllegalStateException("Not a instance-value specific container of an opcode");
	}

	public boolean getopt(byte mask) {
	    throw new IllegalStateException("Not an instance-value specific container of an opcode");
	}

	public byte getword() {
	    return this.typeword;
	}

	public int args() {
	    return (this.args != null) ? this.args.size() : -1;
	}

	public OpcodeArgType argtype(int slot) {
	    return (this.args != null) ? this.args.get(slot) : null;
	}

	public Object argobject(int slot) {
	    throw new IllegalStateException("Not an instance-value specific container of an opcode");
	}

    }

    /**
     * Opcode value box where the opcode defines either a control bitmask, a set of paramters or both a control bitmask and parameters which
     * references one of the global opcode type instance singleton(s).
     */
    public static class OpcodeControlBitmask implements Opcode {

	// which opcode the op really is, usually a reference to enum:Opcodes
	Opcode baseOp;
	// the value of the mask nibble, if one is present
	byte theMaskNibble;
	// the param values, if any are set
	List<Object> theParamValueList;

	public OpcodeControlBitmask(Opcode theTypeOfOpcodeReal, List<Object> theParamValues) throws InstantiationException {
	    this(theTypeOfOpcodeReal, (byte) 0x00, theParamValues);
	}

	public OpcodeControlBitmask(Opcode theTypeOfOpcodeReal, byte theMaskNibble) throws InstantiationException {
	    this(theTypeOfOpcodeReal, theMaskNibble, null);
	}

	public OpcodeControlBitmask(Opcode theTypeOfOpcodeReal, byte theMaskNibble, List<Object> theParamValues) throws InstantiationException {
	    super();
	    this.baseOp = theTypeOfOpcodeReal;
	    this.theMaskNibble = theMaskNibble;
	    this.theParamValueList = theParamValues;

	    if (this.theParamValueList != null && this.theParamValueList.size() != 0) {
		if (this.theParamValueList.size() != this.args())
		    throw new InstantiationException("Wrong opcode argument count");
		for (int i = 0, j = this.args(); i < j; i++)
		    switch (this.argtype(i)) {
			case BOOL_LITERAL:
			    if (!(this.theParamValueList.get(i) instanceof Boolean))
				throw new InstantiationException(String.format("Expected Boolean in slot %s, got %s", i,
					this.theParamValueList.get(i).getClass().getName()));
			    break;
			case INT_LITERAL:
			case INT_PACKED:
			    if (!(this.theParamValueList.get(i) instanceof Integer))
				throw new InstantiationException(String.format("Expected Integer in slot %s, got %s", i,
					this.theParamValueList.get(i).getClass().getName()));
			    break;

			case REAL_LITERAL:
			    if (!(this.theParamValueList.get(i) instanceof Double))
				throw new InstantiationException(String.format("Expected Double in slot %s, got %s", i,
					this.theParamValueList.get(i).getClass().getName()));
			    break;

			case STR_LITERAL:
			    if (!(this.theParamValueList.get(i) instanceof String))
				throw new InstantiationException(String.format("Expected String in slot %s, got %s", i,
					this.theParamValueList.get(i).getClass().getName()));
			    break;
		    }
	    }
	}

	@Override
	public Opcodes opcode() {
	    return (baseOp instanceof Opcodes) ? (Opcodes) baseOp : baseOp.opcode();
	}

	public boolean hasopts() {
	    return baseOp.hasopts();
	}

	public byte getopts() {
	    return theMaskNibble;
	}

	public boolean getopt(byte mask) {
	    return (theMaskNibble & mask) != 0;
	}

	public byte getword() {
	    return baseOp.getword();
	}

	public int args() {
	    return baseOp.args();
	}

	public OpcodeArgType argtype(int slot) {
	    return baseOp.argtype(slot);
	}

	public Object argobject(int slot) {
	    return (theParamValueList != null) ? theParamValueList.get(slot) : null;
	}

    }

    // extension of the value box type which allows the value of integer values
    // to be deferred to a 'label memory address location' instead of a value
    // already known
    public class OpcodeControlBitmaskLabel extends OpcodeControlBitmask {

	List<String> theParamLabelList;

	public OpcodeControlBitmaskLabel(Opcode theTypeOfOpcodeReal, List<Object> theParamValues, List<String> theParamLabels)
		throws InstantiationException {
	    super(theTypeOfOpcodeReal, (byte) 0x00, theParamValues);
	    this.theParamLabelList = theParamLabels;
	}

	public OpcodeControlBitmaskLabel(Opcode theTypeOfOpcodeReal, byte theMaskNibble)
		throws InstantiationException {
	    super(theTypeOfOpcodeReal, theMaskNibble, null);
	}

	public OpcodeControlBitmaskLabel(Opcode theTypeOfOpcodeReal, byte theMaskNibble, List<Object> theParamValues,
		List<String> theParamLabels)
		throws InstantiationException {
	    super(theTypeOfOpcodeReal, theMaskNibble, theParamValues);
	    this.theParamLabelList = theParamLabels;
	}

	public String arglabel(int slot) {
	    return (theParamLabelList != null) ? theParamLabelList.get(slot) : null;
	}

	public void setargobject(int slot, Object val) {
	    theParamValueList.set(slot, val);
	}

    }

}
