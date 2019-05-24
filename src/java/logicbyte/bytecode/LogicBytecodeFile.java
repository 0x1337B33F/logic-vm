package logicbyte.bytecode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LogicBytecodeFile {

    public static class BytecodeFunctionDecl {

	public static class DebugLocationSegment {

	    public final int line, col;

	    public DebugLocationSegment() {
		this(-1, -1);
	    }

	    public DebugLocationSegment(int line, int col) {
		this.line = line;
		this.col = col;
	    }
	}

	public static class DebugSegment {

	    int numArgNames, numLocalNames;

	    String[] argNameTable;
	    String[] localNameTable;

	    int numOfLocationTags;
	    int[] locationTagAddrs;
	    DebugLocationSegment[] locationTags;

	    public DebugSegment() {
	    }

	    public void setargnames(String[] table) {
		this.argNameTable = table;
		this.numArgNames = (this.argNameTable != null) ? this.argNameTable.length : 0;
	    }

	    public void setlocalnames(String[] table) {
		this.localNameTable = table;
		this.numLocalNames = (this.localNameTable != null) ? this.localNameTable.length : 0;
	    }

	    public void setlocations(int[] addrs, DebugLocationSegment[] tags) {
		if ((addrs == null && tags != null) || (addrs != null && tags == null))
		    throw new IllegalArgumentException("addrs and tags must be both defined or undefined");
		if (addrs == null) {
		    this.locationTagAddrs = null;
		    this.locationTags = null;
		    this.numOfLocationTags = 0;
		} else {
		    if (addrs.length != tags.length)
			throw new IllegalArgumentException("addrs and tags must be of same size");
		    this.locationTagAddrs = addrs;
		    this.locationTags = tags;
		    this.numOfLocationTags = this.locationTagAddrs.length;
		}
	    }

	    public boolean hasargnames() {
		return this.numArgNames != 0;
	    }

	    public boolean haslocalnames() {
		return this.numLocalNames != 0;
	    }

	    public int numargnames() {
		return this.numArgNames;
	    }

	    public int numlocalnames() {
		return this.numLocalNames;
	    }

	    public String argname(int slot) {
		return this.argNameTable[slot];
	    }

	    public String localname(int slot) {
		return this.localNameTable[slot];
	    }

	    public boolean haslocations() {
		return this.numOfLocationTags != 0;
	    }

	    public int numlocations() {
		return this.numOfLocationTags;
	    }

	    public int locaddr(int slot) {
		return this.locationTagAddrs[slot];
	    }

	    public DebugLocationSegment loctag(int slot) {
		return this.locationTags[slot];
	    }

	}

	/**
	 * Mask used to indicate the function is a real (user) function.
	 */
	public static final int FUNC_TYPE_USER = 0b00000000;
	/**
	 * Mask used to indicate the function is a native (external) function.
	 */
	public static final int FUNC_TYPE_NATIVE = 0b00000001;

	/**
	 * The acceptable function type bit(s).
	 */
	public static final int FUNC_TYPE = (FUNC_TYPE_USER | FUNC_TYPE_NATIVE);
	/**
	 * The unacceptable function type bit(s).
	 */
	public static final int FUNC_TYPEILLEGAL = ~FUNC_TYPE;

	/**
	 * Mask used to indicate the function has a return value.
	 */
	public static final int FUNC_MASK_HASRET = 0b00000001;
	/**
	 * Mask used to indicate the function has arguments.
	 */
	public static final int FUNC_MASK_HASARGS = 0b00000010;
	/**
	 * Mask used to indicate the function declares locals.
	 */
	public static final int FUNC_MASK_HASLOCALS = 0b00000100;
	/**
	 * Mask used to indicate the function returns an array of the return type; otherwise, returns a value of the return type (literal).
	 */
	public static final int FUNC_MASK_HASRETARR = 0b00001000;
	/**
	 * Mask used to indicate the function has a debug data attachment.
	 */
	public static final int FUNC_MASK_HASDEBUG = 0b10000000;

	/**
	 * The acceptable function mask bit(s).
	 */
	public static final int FUNC_MASK = (FUNC_MASK_HASRET
		| FUNC_MASK_HASARGS
		| FUNC_MASK_HASLOCALS
		| FUNC_MASK_HASRETARR
		| FUNC_MASK_HASDEBUG);
	/**
	 * The unacceptable function mask bit(s).
	 */
	public static final int FUNC_MASKILLEGAL = ~FUNC_MASK;

	/**
	 * The function type, one of FUNC_TYPE_USER or FUNC_TYPE_NATIVE
	 */
	final int fnType;

	/**
	 * The function mask, of ( FUNC_MASK_HASRET, FUNC_MASK_HASARGS, FUNC_MASK_HASLOCALS, FUNC_MASK_HASRETARR, FUNC_MASK_HASDEBUG )
	 */
	int fnMask;

	/**
	 * The function name
	 */
	final String fnName;

	/**
	 * The return type of the function
	 */
	int returnType;

	/**
	 * The number of function arguments
	 */
	int numArgTypes;

	/**
	 * The function argument type(s)
	 */
	int[] argTypes;

	/**
	 * The function argument array type(s)
	 */
	boolean[] argArrayTypes;

	/**
	 * The number of function locals
	 */
	int numLocalTypes;

	/**
	 * The function local type(s)
	 */
	int[] localTypes;

	/**
	 * The function local array flag(s)
	 */
	boolean[] localArrays;

	/**
	 * The function bytecode body
	 */
	FunctionOpcodeList fnBlock;

	/**
	 * The function debug data attachment
	 */
	DebugSegment debugInfo;

	public BytecodeFunctionDecl(int fnType, String fnName) {
	    this(fnType, fnName, null);
	}

	public BytecodeFunctionDecl(int fnType, String fnName, FunctionOpcodeList ops) {
	    if ((fnType & FUNC_TYPEILLEGAL) != 0)
		throw new IllegalArgumentException(String.format("bits set in fnType for %s which are not defined", fnName));

	    if ((fnType & FUNC_TYPE_NATIVE) != 0) {
		if (ops != null)
		    throw new IllegalArgumentException(String.format("native function %s may not define function opcode(s)", fnName));
	    } else if (ops == null)
		this.fnBlock = new FunctionOpcodeList();
	    else
		this.fnBlock = ops;
	    this.fnType = fnType;
	    this.fnName = fnName;
	}

	/**
	 * @return The name of the function
	 */
	public String name() {
	    return this.fnName;
	}

	/**
	 * @return The type of the function
	 */
	public int typeflag() {
	    return fnType;
	}

	/**
	 * @param mask The function type mask
	 *
	 * @return If the mask matches the function type
	 */
	public boolean typeflag(int mask) {
	    return ((fnType & mask) != 0);
	}

	/**
	 * @return The function properties flags
	 */
	public int maskflag() {
	    return fnMask;
	}

	/**
	 * @param mask The function property mask
	 *
	 * @return If the mask matches the function properties flags
	 */
	public boolean maskflag(int mask) {
	    return ((fnMask & mask) != 0);
	}

	/**
	 * @return If the function is a user-code function (real function)
	 */
	public boolean isufunc() {
	    return !typeflag(FUNC_TYPE_NATIVE);
	}

	/**
	 * @return If the function is a native function
	 */
	public boolean isnfunc() {
	    return typeflag(FUNC_TYPE_NATIVE);
	}

	/**
	 * @return If the function returns any value type
	 */
	public boolean hasreturn() {
	    return maskflag(FUNC_MASK_HASRET);
	}

	/**
	 * @return If the function returns an array of a value type
	 */
	public boolean hasarrayreturn() {
	    return maskflag(FUNC_MASK_HASRETARR);
	}

	/**
	 * @return If the function returns a literal of a value type
	 */
	public boolean hasliteralreturn() {
	    return !maskflag(FUNC_MASK_HASRETARR);
	}

	/**
	 * @return If the function accepts arguments
	 */
	public boolean hasargs() {
	    return maskflag(FUNC_MASK_HASARGS);
	}

	/**
	 * @return If the function has locals
	 */
	public boolean haslocals() {
	    return maskflag(FUNC_MASK_HASLOCALS);
	}

	/**
	 * @return If the function has debug data attached
	 */
	public boolean hasdebug() {
	    return maskflag(FUNC_MASK_HASDEBUG);
	}

	/**
	 * @return The return type-number of the function
	 */
	public int returntype() {
	    return this.returnType;
	}

	/**
	 * @return The number of arguments the function accepts
	 */
	public int numargs() {
	    return this.numArgTypes;
	}

	/**
	 * @param slot The argument slot number
	 *
	 * @return The type-number of the argument in the slot specified
	 */
	public int argtype(int slot) {
	    return this.argTypes[slot];
	}

	/**
	 * @param slot The argument slot number
	 *
	 * @return If the argument is an array of the type; otherwise, a literal of the type
	 */
	public boolean argarray(int slot) {
	    return this.argArrayTypes[slot];
	}

	/**
	 * @return The number of locals the function declares
	 */
	public int numlocals() {
	    return this.numLocalTypes;
	}

	/**
	 * @param slot The local slot number
	 *
	 * @return The type-number of the local in the slot specified
	 */
	public int localtype(int slot) {
	    return this.localTypes[slot];
	}

	/**
	 * @param slot The local slot number
	 *
	 * @return If the local in the slot specified is an array or not
	 */
	public boolean localarray(int slot) {
	    return this.localArrays[slot];
	}

	/**
	 * @return The function opcode list
	 */
	public FunctionOpcodeList opcodes() {
	    return this.fnBlock;
	}

	/**
	 * @return The function debug data attachment
	 */
	public DebugSegment debug() {
	    return this.debugInfo;
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append(this.getClass().getName()).append("{");
	    sb.append("name=").append(this.fnName).append(",");
	    sb.append("type=").append(this.fnType).append(",");
	    sb.append("mask=").append(this.fnMask).append(",");
	    sb.append("return=").append(this.returnType).append(",");
	    sb.append("numargs=").append(this.numArgTypes).append(",");
	    sb.append("numlocals=").append(this.numLocalTypes).append(",");
	    sb.append("hasdebug=").append((this.debugInfo != null) ? "true" : "false").append(",");
	    sb.append("\r\nbytecode=").append(this.fnBlock.toString());
	    return sb.append("}").toString();
	}

	@Override
	public boolean equals(Object obj) {
	    if (!(obj instanceof BytecodeFunctionDecl))
		return false;
	    BytecodeFunctionDecl other = (BytecodeFunctionDecl) obj;

	    if (this.fnType != other.fnType
		    || !this.fnName.equals(other.fnName)
		    || this.fnMask != other.fnMask
		    || this.returnType != other.returnType
		    || this.numArgTypes != other.numArgTypes
		    || this.numLocalTypes != other.numLocalTypes)
		return false;

	    if (this.numArgTypes > 0)
		for (int i = 0, j = this.numArgTypes; i < j; i++)
		    if (this.argTypes[i] != other.argTypes[i]
			    || this.argArrayTypes[i] != other.argArrayTypes[i])
			return false;

	    if (this.numLocalTypes > 0)
		for (int i = 0, j = this.numLocalTypes; i < j; i++)
		    if (this.localTypes[i] != other.localTypes[i]
			    || this.localArrays[i] != other.localArrays[i])
			return false;

	    if (this.fnBlock == null && other.fnBlock == null)
		return true;
	    if (this.fnBlock == null && other.fnBlock != null)
		return false;
	    if (this.fnBlock != null && other.fnBlock == null)
		return false;
	    return this.fnBlock.equals(other.fnBlock);
	}

    }

    public static class BytecodeFunctionDeclMutable extends BytecodeFunctionDecl {

	boolean returnsArrayType;

	public BytecodeFunctionDeclMutable(int fnMask, String fnName) {
	    super(fnMask, fnName);
	    this.returnsArrayType = ((fnMask & FUNC_MASK_HASRETARR) != 0);
	}

	public BytecodeFunctionDeclMutable(int fnMask, String fnName, FunctionOpcodeList ops) {
	    super(fnMask, fnName, ops);
	    this.returnsArrayType = ((fnMask & FUNC_MASK_HASRETARR) != 0);
	}

	/**
	 * Set the function's arguments.
	 *
	 * @param table  The list of argument type-number(s), or null if the function does not accept arguments.
	 * @param arrays The list of argument(s) that are array types or literal types of the type in the table for the slot.
	 */
	public void setargs(int[] table, boolean[] arrays) {
	    this.argTypes = table;
	    this.argArrayTypes = arrays;
	    this.numArgTypes = (this.argTypes != null) ? this.argTypes.length : 0;
	    setmaskflag();
	}

	/**
	 * Set the function's locals.
	 *
	 * @param types  The list of local type-number(s), or null if the function does not declare locals.
	 * @param arrays The list of local array indicators, or null if the function does not declare locals.
	 */
	public void setlocals(int[] types, boolean[] arrays) {
	    if (isnfunc())
		throw new IllegalStateException("may not set locals on a native function declaration");
	    if ((types == null && arrays != null)
		    || (types != null && arrays == null)
		    || (types != null && arrays != null && types.length != arrays.length))
		throw new IllegalStateException("types and arrays must be of same dimension");
	    this.localTypes = types;
	    this.localArrays = arrays;
	    this.numLocalTypes = (this.localTypes != null) ? this.localTypes.length : 0;
	    setmaskflag();
	}

	/**
	 * Set the return type-number of the local, or <code>0</code> (meaning nothing).
	 *
	 * @param type  The return type-number of the local, or <code>0</code> (meaning nothing).
	 * @param array If the return type is an array type or a literal of the return value type.
	 */
	public void setreturn(int type, boolean array) {
	    this.returnType = type;
	    this.returnsArrayType = array;
	    setmaskflag();
	}

	/**
	 * Set the debug data attachment of the function, or null if the function does not require a debug data attachment.
	 *
	 * @param segment The debug data attachment, or <code>null</code>.
	 */
	public void setdebug(DebugSegment segment) {
	    this.debugInfo = segment;
	    setmaskflag();
	}

	void setmaskflag() {
	    int newflag = 0b00000000;
	    if (this.numLocalTypes != 0)
		newflag |= FUNC_MASK_HASLOCALS;
	    if (this.numArgTypes != 0)
		newflag |= FUNC_MASK_HASARGS;
	    if (this.returnType != 0)
		newflag |= FUNC_MASK_HASRET;
	    if ( this.returnsArrayType )
		newflag |= FUNC_MASK_HASRETARR;
	    if (this.debugInfo != null)
		newflag |= FUNC_MASK_HASDEBUG;
	    this.fnMask = newflag;
	}
    }

    final Object mutatorLock = new Object();

    final String sourceName;

    private List<BytecodeFunctionDecl> functionDeclList = new LinkedList<>();
    private Map<String, BytecodeFunctionDecl> functionDeclIndex = new HashMap<>();

    public LogicBytecodeFile(String sourceName) {
	this.sourceName = sourceName;
	this.functionDeclList = new LinkedList<>();
	this.functionDeclIndex = new HashMap<>();
    }

    public String source() {
	return this.sourceName;
    }

    public boolean add(BytecodeFunctionDecl d) {
	synchronized ( mutatorLock ) {
	    if (!this.functionDeclList.contains(d)) {
		this.functionDeclList.add(d);
		this.functionDeclIndex.put(d.name(), d);
		return true;
	    } else
		return false;
	}
    }

    public BytecodeFunctionDecl get(String nameOfFunc) {
	synchronized ( mutatorLock ) {
	    return this.functionDeclIndex.get(nameOfFunc);
	}
    }

    public BytecodeFunctionDecl[] getAll() {
	synchronized ( mutatorLock ) {
	    return this.functionDeclList.toArray(new BytecodeFunctionDecl[ 0 ]);
	}
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(this.getClass().getName()).append("{");
	sb.append("\tfunctionDeclList=[\r\n");

	for (int i = 0, j = this.functionDeclList.size(); i < j; i++) {
	    BytecodeFunctionDecl decl = this.functionDeclList.get(i);
	    sb.append("\t\t").append(decl.toString());
	    if (i < (j - 1))
		sb.append(",\r\n");
	}
	sb.append("\r\n\t]");
	return sb.append("}").toString();
    }

}
