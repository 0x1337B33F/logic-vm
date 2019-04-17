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

	public static final int FUNC_TYPE_USER = 0b00000000;
	public static final int FUNC_TYPE_NATIVE = 0b00000001;

	public static final int FUNC_TYPE = (FUNC_TYPE_USER | FUNC_TYPE_NATIVE);
	public static final int FUNC_TYPEILLEGAL = ~FUNC_TYPE;

	public static final int FUNC_MASK_HASRET = 0b00000001;
	public static final int FUNC_MASK_HASARGS = 0b00000010;
	public static final int FUNC_MASK_HASLOCALS = 0b00000100;
	public static final int FUNC_MASK_HASDEBUG = 0b10000000;

	public static final int FUNC_MASK = (FUNC_MASK_HASRET
		| FUNC_MASK_HASARGS
		| FUNC_MASK_HASLOCALS
		| FUNC_MASK_HASDEBUG);
	public static final int FUNC_MASKILLEGAL = ~FUNC_MASK;

	final int fnType;

	int fnMask;

	final String fnName;

	int returnType;

	int numArgTypes;

	int[] argTypes;

	int numLocalTypes;

	int[] localTypes;

	boolean[] localArrays;

	FunctionOpcodeList fnBlock;

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

	public String name() {
	    return this.fnName;
	}

	public int typeflag() {
	    return fnType;
	}

	public boolean typeflag(int mask) {
	    return ((fnType & mask) != 0);
	}

	public int maskflag() {
	    return fnMask;
	}

	public boolean maskflag(int mask) {
	    return ((fnMask & mask) != 0);
	}

	public boolean isufunc() {
	    return !typeflag(FUNC_TYPE_NATIVE);
	}

	public boolean isnfunc() {
	    return typeflag(FUNC_TYPE_NATIVE);
	}

	public boolean hasreturn() {
	    return maskflag(FUNC_MASK_HASRET);
	}

	public boolean hasargs() {
	    return maskflag(FUNC_MASK_HASARGS);
	}

	public boolean haslocals() {
	    return maskflag(FUNC_MASK_HASLOCALS);
	}

	public boolean hasdebug() {
	    return maskflag(FUNC_MASK_HASDEBUG);
	}

	public int returntype() {
	    return this.returnType;
	}

	public int numargs() {
	    return this.numArgTypes;
	}

	public int argtype(int slot) {
	    return this.argTypes[slot];
	}

	public int numlocals() {
	    return this.numLocalTypes;
	}

	public int localtype(int slot) {
	    return this.localTypes[slot];
	}

	public boolean localarray(int slot) {
	    return this.localArrays[slot];
	}

	public FunctionOpcodeList opcodes() {
	    return this.fnBlock;
	}

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
		    if (this.argTypes[i] != other.argTypes[i])
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

	public BytecodeFunctionDeclMutable(int fnMask, String fnName) {
	    super(fnMask, fnName);
	}

	public BytecodeFunctionDeclMutable(int fnMask, String fnName, FunctionOpcodeList ops) {
	    super(fnMask, fnName, ops);
	}

	public void setargs(int[] table) {
	    this.argTypes = table;
	    this.numArgTypes = (this.argTypes != null) ? this.argTypes.length : 0;
	    setmaskflag();
	}

	public void setlocals(int[] types, boolean[] arrays) {
	    if (isnfunc())
		throw new IllegalStateException("may not set locals on a native function declaration");
	    if (types.length != arrays.length)
		throw new IllegalStateException("types and arrays must be of same dimension");
	    this.localTypes = types;
	    this.localArrays = arrays;
	    this.numLocalTypes = (this.localTypes != null) ? this.localTypes.length : 0;
	    setmaskflag();
	}

	public void setreturn(int type) {
	    this.returnType = type;
	    setmaskflag();
	}

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
	    if (this.debugInfo != null)
		newflag |= FUNC_MASK_HASDEBUG;
	    this.fnMask = newflag;
	}
    }

    final Object mutatorLock = new Object();

    List<BytecodeFunctionDecl> functionDeclList = new LinkedList<>();
    Map<String, BytecodeFunctionDecl> functionDeclIndex = new HashMap<>();

    public LogicBytecodeFile() {
	this.functionDeclList = new LinkedList<>();
	this.functionDeclIndex = new HashMap<>();
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
