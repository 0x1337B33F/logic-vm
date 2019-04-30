package logicscript.assembler.writer;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import logicbyte.bytecode.FunctionOpcodeList;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugLocationSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDecl.DebugSegment;
import logicbyte.bytecode.LogicBytecodeFile.BytecodeFunctionDeclMutable;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.OpcodeControlBitmask;
import logicbyte.bytecode.Opcode.OpcodeControlBitmaskLabel;
import logicbyte.bytecode.Opcode.Opcodes;
import logicbyte.bytecode.OpcodeParamValueTable.BinaryExpressionParamValueTable;
import logicscript.assembler.ExpressionParamConverter;
import logicscript.assembler.FunctionBytecodeTransformer;
import logicscript.assembler.writer.TypeAssembler.TypeAssemblerLookupException;
import logicscript.ast.Chunk;
import logicscript.ast.Location;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.BinaryExpression;
import logicscript.ast.nodes.expr.Expression;
import logicscript.ast.nodes.expr.FunctionCallExpression;
import logicscript.ast.nodes.expr.FunctionCallVarExpression;
import logicscript.ast.nodes.expr.FunctionRefExpression;
import logicscript.ast.nodes.expr.LiteralValue;
import logicscript.ast.nodes.expr.ParenExpression;
import logicscript.ast.nodes.expr.UnaryExpression;
import logicscript.ast.nodes.expr.VariableArrayRefExpression;
import logicscript.ast.nodes.expr.VariableRefExpression;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.ControlFlowStatement;
import logicscript.ast.nodes.stmt.DirectAssembly;
import logicscript.ast.nodes.stmt.ExecutionFunctionStatement;
import logicscript.ast.nodes.stmt.Statement;
import logicscript.ast.nodes.stmt.cfl.ExitWhenStatement;
import logicscript.ast.nodes.stmt.cfl.ForStatement;
import logicscript.ast.nodes.stmt.cfl.IfStatement;
import logicscript.ast.nodes.stmt.cfl.LoopStatement;
import logicscript.ast.nodes.stmt.cfl.RaiseStatement;
import logicscript.ast.nodes.stmt.cfl.ReturnStatement;
import logicscript.ast.nodes.stmt.cfl.TryCatchStatement;
import logicscript.ast.nodes.stmt.cfl.YieldStatement;
import logicscript.ast.nodes.stmt.ef.DebugStatement;
import logicscript.ast.nodes.stmt.ef.FunctionCallStatement;
import logicscript.ast.nodes.stmt.ef.FunctionCallVarStatement;
import logicscript.ast.nodes.stmt.ef.SetArrayVariableStatement;
import logicscript.ast.nodes.stmt.ef.SetVariableStatement;
import logictype.BiDiMap;
import logictype.Pair;
import logictype.Tuple;

/**
 * <p>
 * Performs AST function-declaration assembly using an internal function assembly state machine.</p>
 *
 * @author Lochie
 */
public class FunctionAssembler {

    public static class FunctionAssemblySourceException extends Exception {

	Chunk declChunk = null;
	FunctionDecl declFunc = null;
	Statement declStmt = null;
	Expression declExpr = null;

	public FunctionAssemblySourceException(Chunk declChunk, String message) {
	    this(declChunk, null, message);
	}

	public FunctionAssemblySourceException(Chunk declChunk, FunctionDecl declFunc, String message) {
	    super(message);
	    this.declChunk = declChunk;
	    this.declFunc = declFunc;
	}

	public FunctionAssemblySourceException(Chunk declChunk, FunctionDecl declFunc, Statement declStmt, String message) {
	    super(message);
	    this.declChunk = declChunk;
	    this.declFunc = declFunc;
	    this.declStmt = declStmt;
	}

	public FunctionAssemblySourceException(Chunk declChunk, FunctionDecl declFunc, Expression declExpr, String message) {
	    super(message);
	    this.declChunk = declChunk;
	    this.declFunc = declFunc;
	    this.declExpr = declExpr;
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder(getClass().getName());
	    sb.append(": ").append(getMessage()).append("\r\n");
	    if (declExpr != null)
		sb.append("\tat expression: `").append(declExpr.toString()).append("`, ").append(declExpr.location.toString()).
			append("\r\n");
	    if (declStmt != null)
		sb.append("\tat statement: `").append(declStmt.toString()).append("`, ").append(declStmt.location.toString()).append("\r\n");
	    if (declFunc != null)
		sb.append("\tin function: `").append(declFunc.toString()).append("`, ").append(declFunc.location.toString()).append("\r\n");
	    if (declChunk != null)
		sb.append("\tin chunk: ").append(declChunk.toString()).append("\r\n");
	    return sb.toString();
	}

    }

    /**
     * <p>
     * The main function assembly machine. Generates function bytecode from the input AST, placing the generated opcodes or labels (which
     * shall be translated to addresses) on the output tape. The output tape of bytecode is finally streamed to the destination struct
     * object, which shall result in the translation of all label-references to addresses using a single pass.</p>
     *
     * <p>
     * The assembly machine is stateful; the machine expects to be placed in the following states:</p>
     * <ul>
     * <li>At least one call to either {@link #initUserFn()} or {@link #initNativeFn()}, which shall place the machine in a state ready for
     * assembly;</li>
     * <li>A call to
     * {@link #emit(logicscript.assembler.writer.TypeAssembler, logicbyte.bytecode.LinkerObjectFile, logicscript.ast.Chunk, logicscript.ast.nodes.decl.FunctionDecl)},
     * which shall accept the provided AST function declaration and translate the chunk to bytecode on the output tape, or place the state
     * machine in an error state; then</li>
     * <li>
     * <ul>
     * <li>Generated bytecode may be translated from label references to finalized absolute bytecode offsets using the
     * {@link #stream(logicbyte.bytecode.LogicBytecodeFile)} method, which shall finalize all references and generate final function
     * bytecode to the destination bytecode object; or</li>
     * <li>The machine may be reset, discarding all output tape using the {@link #reset()} method. The machine shall be placed in the idle
     * state for another compilation task.</li>
     * </ul>
     * </li>
     * </ul>
     */
    public static class FunctionAssemblyGeneratorMachine {

	static final int s_maxFuncClosureVars = 128;
	static final int s_maxFuncSubClosureVars = 16;

	/**
	 * abstract of GeneratorBlock
	 */
	interface GeneratorBlock {

	    /**
	     * lended from Ljava/lang/Enum#name(V)Ljava/lang/String;
	     *
	     * @return
	     */
	    public String name();

	    /**
	     * lended from Ljava/lang/Enum#ordinal(v)I;
	     *
	     * @return
	     */
	    public int ordinal();

	    /**
	     * Gets the assigned label of the block node, if one is assigned; otherwise, returns <code>null</code>.
	     *
	     * @return The label of the block (only if the block is really a value box and not an enumeration singleton)
	     */
	    public String label();
	}

	/**
	 * singleton for blocks which don't have labels, therefore do not need value boxing and can be represented as an enum const
	 */
	enum BlockType implements GeneratorBlock {
	    IF, ELSEIF, ELSE, FOR, LOOP, TRY, CATCH;
	    // implements name() Ljava/lang/String; implicit from Enum as klazz
	    // implements ordinal() Ljava/lang/Integer; implicit from Enum as klazz

	    /**
	     * @see GeneratorBlock#label()
	     * @return <code>null</code>
	     */
	    @Override
	    public String label() {
		return null;
	    }
	}

	/**
	 * implementation of the block type which accepts an inner type to provide label value boxing for only instances of block(s) which
	 * have labels assigned; otherwise, the value should be one of the enum const objects instead
	 */
	class BlockTypeLabel implements GeneratorBlock {

	    final BlockType baseType;
	    final String labelRef;

	    /**
	     * Value box object constructor
	     *
	     * @param theBaseType        The base type singleton
	     * @param theLabelOfTheBlock The label which has been assigned to the generated block; the label may be the begin or end label
	     *                           depending on the type of the block
	     */
	    public BlockTypeLabel(BlockType theBaseType, String theLabelOfTheBlock) {
		this.baseType = theBaseType;
		this.labelRef = theLabelOfTheBlock;
	    }

	    @Override
	    public String name() {
		return this.baseType.name();
	    }

	    @Override
	    public int ordinal() {
		return this.baseType.ordinal();
	    }

	    @Override
	    public String label() {
		return this.labelRef;
	    }

	}

	/**
	 * Implements the local variable scope ('closure') struct.
	 */
	class GeneratorClosure {

	    /**
	     * generator hint name of the closure
	     */
	    String label;

	    /**
	     * the starting varslot number
	     */
	    int start;
	    /**
	     * the number of varslot(s)
	     */
	    int count;
	    /**
	     * the type(s) of each slot:
	     */
	    int[] types;
	    /**
	     * the name of each slot:
	     */
	    String[] names;
	    /**
	     * the array state of each slot:
	     */
	    boolean[] arrays;

	    // macros:
	    /**
	     * Check if a name is a real var in current scope
	     *
	     * @param s Name of var
	     *
	     * @return Whether the var is defined as an lvar in the current scope
	     */
	    boolean checkname_real(String s) {
		return (slot(s) != -1);
	    }

	    /**
	     * Check if a name has not yet been defined in the current scope
	     *
	     * @param s Name of var
	     *
	     * @return Whether the var is not yet defined as an lvar in the current scope; does not compute if the name shall shadow an
	     *         already defined name
	     */
	    boolean checkname_distinct(String s) {
		return (slot(s) == -1);
	    }

	    /**
	     * Get slot of variable, or -1 if it isn't explicitly defined by the current scope
	     *
	     * @param s Name of var
	     *
	     * @return local vars slot of closure-temp var, or <code>-1</code> if undefined lvar name in local scope; does not check higher
	     *         level closure(s) for existing lvar
	     */
	    int slot(String s) {
		if (count == 0)
		    return -1;
		for (int i = 0, j = count; i < j; i++)
		    if (names[i].equals(s))
			return i;
		return -1;
	    }
	}

	volatile boolean /**
		 * If debug symbols should be output at all
		 */
		m_generateDebugSymbols = true,
		/**
		 * If currently in state ASSEMBLE
		 */
		m_isAssembling = false,
		/**
		 * If currently writing a NATIVE FUNCTION
		 */
		m_isNativeFunction = false,
		/**
		 * If currently in state ASSEMBLED
		 */
		m_hasAssembled = false;

	/**
	 * The generated function object instance
	 */
	LogicBytecodeFile.BytecodeFunctionDeclMutable m_generatingFunctionObj;

	/**
	 * The generated function debug object instance
	 */
	LogicBytecodeFile.BytecodeFunctionDeclMutable.DebugSegment m_generatingFunctionDebugObj;

	/**
	 * The `output tape' of the machine:
	 */
	List< Pair< Object, DebugLocationSegment>> m_generatedObjectList;

	/**
	 * The current stack of closure(s) in scope, excluding the top-level function closure
	 */
	Stack< GeneratorClosure> m_generatedClosures = new Stack<>();

	/**
	 * The top-level function closure
	 */
	GeneratorClosure m_generatedFunctionClosure;

	/**
	 * The current control-flow block stack
	 */
	Stack<GeneratorBlock> m_generatedBlockStack = new Stack<>();

	/**
	 * The current LOOP label stack
	 */
	Stack<String> m_generatedLoopLabelStack = new Stack<>();

	List<FunctionBytecodeTransformer> m_transformers = new LinkedList<>();

	public FunctionAssemblyGeneratorMachine() {
	    this.m_generatedObjectList = new LinkedList<>();
	}

	/**
	 * Reset the state machine. The output tape, and all internal state registers shall be cleared.
	 *
	 * @return The generator machine instance.
	 */
	FunctionAssemblyGeneratorMachine reset() {
	    m_generatingFunctionObj = null;
	    m_generatingFunctionDebugObj = null;
	    m_generatedObjectList.clear();

	    m_generatedFunctionClosure = null;
	    m_generatedClosures.clear();

	    m_isAssembling = false;
	    m_hasAssembled = false;
	    return this;
	}

	FunctionAssemblyGeneratorMachine addTransformer(FunctionBytecodeTransformer t) {
	    this.m_transformers.add(t);
	    return this;
	}

	/**
	 * Set the state machine debug option.
	 *
	 * @param f If debugging symbols should be generated or not.
	 *
	 * @return The generator machine instance.
	 */
	FunctionAssemblyGeneratorMachine initShouldDebug(boolean f) {
	    m_generateDebugSymbols = f;
	    return this;
	}

	/**
	 * Initialize the state machine so that it is ready to compile a user-defined function (which contains zero or more statement(s)).
	 *
	 * @return The generator machine instance.
	 */
	FunctionAssemblyGeneratorMachine initUserFn() {
	    init(false);
	    return this;
	}

	/**
	 * Initialize the state machine so that it is ready to compile a native-defined function (which contains no statement(s), and is a
	 * synthetic wrapper around a Java instance method, static method or anonymous lambda function).
	 *
	 * @return The generator machine instance.
	 */
	FunctionAssemblyGeneratorMachine initNativeFn() {
	    init(true);
	    return this;
	}

	/**
	 * State-machine initializer method.
	 *
	 * @param fnIsNative If the function is a native function or not.
	 */
	void init(boolean fnIsNative) {
	    if (m_isAssembling)
		throw new IllegalStateException("already assembling");
	    if (m_hasAssembled)
		throw new IllegalStateException("already assembled");
	    m_isAssembling = true;
	    m_isNativeFunction = fnIsNative;
	}

	/**
	 * Begins assembly of the provided {@link FunctionDecl} to the output tape.
	 *
	 * @param typeMachine    The type assembler instance.
	 * @param linker         The destination linker struct.
	 * @param declaringChunk The AST chunk which defined the FunctionDecl.
	 * @param function       The FunctionDecl to assemble.
	 *
	 * @return If the function was successfully assembled; depending on the type of function, a number of opcodes may reside on the
	 *         output tape.
	 *
	 * @throws java.lang.InstantiationException
	 * @throws logicscript.assembler.writer.TypeAssembler.TypeAssemblerLookupException
	 * @throws logicscript.assembler.writer.FunctionAssembler.FunctionAssemblySourceException If the function machine is in an invalid
	 *                                                                                        state, or, if the function statements are
	 *                                                                                        not acceptable, or, should some other
	 *                                                                                        assembly exception occur, one of
	 *                                                                                        {@link IllegalStateException} or
	 *                                                                                        {@link FunctionAssemblySourceException}
	 *                                                                                        will be thrown.
	 */
	boolean emit(TypeAssembler typeMachine, LinkerObjectFile linker, Chunk declaringChunk, FunctionDecl function)
		throws IllegalStateException, InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	    if (!m_isAssembling)
		throw new IllegalStateException("not assembling");
	    if (m_hasAssembled)
		throw new IllegalStateException("already assembled");

	    m_generatingFunctionObj = new BytecodeFunctionDeclMutable(
		    ((m_isNativeFunction) ? BytecodeFunctionDecl.FUNC_TYPE_NATIVE : BytecodeFunctionDecl.FUNC_TYPE_USER),
		    function.name);

	    // allocate function closure object:
	    m_generatedFunctionClosure = new GeneratorClosure();
	    m_generatedFunctionClosure.count = m_generatedFunctionClosure.start = 0;

	    // gobble memory:
	    m_generatedFunctionClosure.names = new String[ s_maxFuncClosureVars ];
	    m_generatedFunctionClosure.types = new int[ s_maxFuncClosureVars ];
	    m_generatedFunctionClosure.arrays = new boolean[ s_maxFuncClosureVars ];

	    // The params become the first function closure value(s): 
	    if (function.paramNames != null || function.paramTypes != null)
		if (!function.paramNames.isEmpty() || !function.paramTypes.isEmpty()) {
		    if (function.paramNames.size() != function.paramTypes.size())
			throw new FunctionAssemblySourceException(declaringChunk, function,
				"incorrect number of function parameter name(s) & type(s)");

		    Iterator< String> itx = function.paramNames.iterator();
		    Iterator< TypeDecl> itx1 = function.paramTypes.iterator();
		    int[] paramTypeNumberTable = new int[ function.paramTypes.size() ];
		    int i = 0;
		    while (itx.hasNext() && itx1.hasNext()) {
			String nameOfParam = itx.next();
			if (!m_generatedFunctionClosure.checkname_distinct(nameOfParam))
			    throw new IllegalStateException("duplicate parameter name");
			TypeDecl typeOfParam = itx1.next();
			int numberOfTypeOfParam = typeMachine.getTypeNumber(typeOfParam);
			int ptr = m_generatedFunctionClosure.count;
			if (ptr >= s_maxFuncClosureVars)
			    throw new FunctionAssemblySourceException(declaringChunk, function, "too many parameters");
			++m_generatedFunctionClosure.count; // lock slot ref
			m_generatedFunctionClosure.names[ptr] = nameOfParam;
			m_generatedFunctionClosure.types[ptr] = numberOfTypeOfParam;
			paramTypeNumberTable[i++] = numberOfTypeOfParam;
		    }
		    m_generatingFunctionObj.setargs(paramTypeNumberTable);
		}

	    // The local var(s) become the subsequent closure value(s):
	    if (function.lvars != null && !function.lvars.isEmpty()) {
		Iterator<FunctionLVarDecl> itx = function.lvars.iterator();

		int[] localTypeNumberTable = new int[ function.lvars.size() ];
		boolean[] localArrayTypeTable = new boolean[ function.lvars.size() ];
		int i = 0;
		while (itx.hasNext()) {
		    FunctionLVarDecl decl = itx.next();
		    if (!m_generatedFunctionClosure.checkname_distinct(decl.nameOfLocalVar))
			throw new FunctionAssemblySourceException(declaringChunk, function, "duplicate local name");
		    int numberOfTypeOfLocal = typeMachine.getTypeNumber(decl.valueType);
		    int ptr = m_generatedFunctionClosure.count;
		    if (ptr >= s_maxFuncClosureVars)
			throw new IllegalStateException("too many locals");
		    ++m_generatedFunctionClosure.count; // lock slot ref
		    m_generatedFunctionClosure.names[ptr] = decl.nameOfLocalVar;
		    m_generatedFunctionClosure.types[ptr] = numberOfTypeOfLocal;
		    m_generatedFunctionClosure.arrays[ptr] = decl.isArrayType;
		    localTypeNumberTable[i] = numberOfTypeOfLocal;
		    localArrayTypeTable[i++] = decl.isArrayType;

		    if (decl.initialRhs != null)
			if (decl.isArrayType) {
			    emitOpcodesExpression(typeMachine, linker, declaringChunk, function, function.block, decl.initialRhs);
			    output(new OpcodeControlBitmask(Opcodes.NEWARRAY, newlist(numberOfTypeOfLocal, ptr)));
			} else {
			    emitOpcodesExpression(typeMachine, linker, declaringChunk, function, function.block, decl.initialRhs);
			    output(new OpcodeControlBitmask(Opcodes.SETLOCAL, newlist(ptr)));
			}
		}

		m_generatingFunctionObj.setlocals(localTypeNumberTable, localArrayTypeTable);
	    }

	    if (function.returnType != null)
		m_generatingFunctionObj.setreturn(typeMachine.getTypeNumber(function.returnType));

	    if (m_generateDebugSymbols) {
		m_generatingFunctionDebugObj = new DebugSegment();
		if (function.paramNames != null && !function.paramNames.isEmpty()) {
		    String[] paramNameTable = new String[ function.paramNames.size() ];
		    for (int i = 0, j = paramNameTable.length; i < j; i++)
			paramNameTable[i] = function.paramNames.get(i);
		    m_generatingFunctionDebugObj.setargnames(paramNameTable);
		}

		if (function.lvars != null && !function.lvars.isEmpty()) {
		    String[] lvarsNameTable = new String[ function.lvars.size() ];
		    for (int i = 0, j = lvarsNameTable.length; i < j; i++)
			lvarsNameTable[i] = function.lvars.get(i).nameOfLocalVar;
		    m_generatingFunctionDebugObj.setlocalnames(lvarsNameTable);
		}
		m_generatingFunctionObj.setdebug(m_generatingFunctionDebugObj);
	    }

	    if (!m_isNativeFunction) {

		if (!emitOpcodes(typeMachine, linker, declaringChunk, function)) {
		    m_hasAssembled = false;
		    throw new IllegalStateException("emit: did not emitOpcodes");
		}

		if (m_generatedObjectList.isEmpty()) {
		    m_hasAssembled = false;
		    throw new IllegalStateException("emit: zero opcodes in function body not allowed");
		}

		if (!m_transformers.isEmpty()) {
		    Iterator<FunctionBytecodeTransformer> itx = m_transformers.iterator();
		    while (itx.hasNext()) {
			FunctionBytecodeTransformer transformer = itx.next();
			transformer.apply(
				m_generatingFunctionObj,
				m_generatingFunctionDebugObj,
				m_generatedObjectList
			);
		    }
		}

		List<Tuple< Integer, Opcode, DebugLocationSegment>> ops = new LinkedList<>();
		Map<String, Integer> labelTable = new HashMap<>();

		Iterator< Pair< Object, DebugLocationSegment>> itx = m_generatedObjectList.iterator();

		int i = 0;
		while (itx.hasNext()) {
		    Pair< Object, DebugLocationSegment> val = itx.next();
		    if (val.a() instanceof String) {
			if (labelTable.containsKey((String) val.a()))
			    throw new IllegalStateException(String.format("emit: duplicate label reference `%s`", val.a()));
			labelTable.put((String) val.a(), i);
		    } else {
			ops.add(new Tuple<>(i, (Opcode) val.a(), val.b()));
			i++;
		    }
		}

		FunctionOpcodeList generatingFunctionOpcodeListPtr = m_generatingFunctionObj.opcodes();

		Iterator< Tuple<Integer, Opcode, DebugLocationSegment>> itx1 = ops.iterator();
		while (itx1.hasNext()) {
		    Tuple<Integer, Opcode, DebugLocationSegment> val = itx1.next();
		    if (val.b() instanceof OpcodeControlBitmaskLabel) {
			OpcodeControlBitmaskLabel opWithLabels = (OpcodeControlBitmaskLabel) val.b();
			if (opWithLabels.args() != 0)
			    for (int j = 0, k = opWithLabels.args(); j < k; j++) {
				String locationLabel = opWithLabels.arglabel(j);
				if (locationLabel != null) {
				    Integer locationOfLabel = labelTable.get(locationLabel);
				    if (locationOfLabel == null) {
					m_hasAssembled = false;
					throw new IllegalStateException("emit: label location from state machine not defined");
				    }
				    opWithLabels.setargobject(j, locationOfLabel);
				}
			    }
		    }
		    generatingFunctionOpcodeListPtr.add(val.b());
		}

		if (m_generateDebugSymbols) {
		    List<Integer> addrList = new LinkedList<>();
		    List<DebugLocationSegment> locList = new LinkedList<>();

		    itx1 = ops.iterator();
		    while (itx1.hasNext()) {
			Tuple<Integer, Opcode, DebugLocationSegment> val = itx1.next();
			if (val.c() != null) {
			    addrList.add(val.a());
			    locList.add(val.c());
			}
		    }

		    int[] addrTable = new int[ addrList.size() ];
		    DebugLocationSegment[] locTable = new DebugLocationSegment[ addrList.size() ];
		    for (int j = 0, k = addrTable.length; j < k; j++) {
			addrTable[j] = addrList.get(j);
			locTable[j] = locList.get(j);
		    }

		    m_generatingFunctionDebugObj.setlocations(addrTable, locTable);
		}

		/*
		 * Final assembly indication is given by confirming that the generating function opcode list
		 * pointer is not empty (ie, the values from the output tape have been moved to the generated
		 * compiled code function).
		 */
		m_hasAssembled = (!generatingFunctionOpcodeListPtr.isEmpty());
	    } else

		// ... it's a native function....
		m_hasAssembled = true;

	    m_isAssembling = false;
	    return m_hasAssembled;
	}

	boolean emitOpcodes(TypeAssembler typeMachine, LinkerObjectFile linker, Chunk declaringChunk, FunctionDecl function)
		throws IllegalStateException, InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	    return emitOpcodesBlock(typeMachine, linker, declaringChunk, function, function.block);
	}

	boolean emitOpcodesBlock(TypeAssembler typeMachine, LinkerObjectFile linker, Chunk declaringChunk, FunctionDecl fnDecl, Block block)
		throws IllegalStateException, InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	    if (block.isEmpty()) {
		output(Opcodes.NOP);
		return true;
	    } else {
		Iterator<Statement> itx = block.iterator();
		while (itx.hasNext()) {
		    Statement stmt = itx.next();
		    if (!emitOpcodesStatement(typeMachine, linker, declaringChunk, fnDecl, block, stmt))
			return false;
		}

		if (block == fnDecl.block)
		    output(Opcodes.NOP);
		return true;
	    }
	}

	boolean emitOpcodesStatement(TypeAssembler typeMachine, LinkerObjectFile linker, Chunk declaringChunk, FunctionDecl fnDecl,
		Block inBlock, Statement s) throws IllegalStateException, InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	    if (s instanceof ControlFlowStatement) {
		if (s instanceof IfStatement) {
		    final String exitifl = label("IF_ENDIF");
		    List<String> exitlabel = newstringlist(exitifl);

		    Expression ifExpr = ((IfStatement) s).conditionalExpr;
		    if (ifExpr instanceof LiteralValue) {
			LiteralValue lv = (LiteralValue) ifExpr;
			if (lv.typeOfValue != TypeDecl.TYPE_BOOLEAN)
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: ifexpr if conditional literal not type=BOOLEAN");

			if (lv == LiteralValue.VALUE_TRUE)
			    return emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, ((IfStatement) s).thenBlock);
		    } else {
			newblocktag(BlockType.IF);
			final String exitl = label("IF_ENDTHEN");

			if (!emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, ifExpr))
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: ifexpr if conditional not on stack");

			output(new OpcodeControlBitmask(Opcodes.CMP, Opcodes.BITMASK_CMP_CONSUME), ifExpr.location);
			output(new OpcodeControlBitmaskLabel(Opcodes.JMP, (byte) (Opcodes.BITMASK_JMP_AORO | Opcodes.BITMASK_JMP_JCJF),
				newlist(-1), newstringlist(exitl)));

			if (!emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, ((IfStatement) s).thenBlock))
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: ifexpr then statements not written");

			output(new OpcodeControlBitmaskLabel(Opcodes.JMP, Opcodes.BITMASK_JMP_AORO, newlist(-1), exitlabel));
			output(exitl);

			endblocktag(BlockType.IF);
		    }

		    if (((IfStatement) s).elseIfCondExprs != null) {
			List<Expression> elseIfCondExprs = ((IfStatement) s).elseIfCondExprs;
			List<Block> elseIfBlocks = ((IfStatement) s).elseIfBlocks;

			if (elseIfCondExprs.size() != elseIfBlocks.size())
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: ifexpr elseif conditional-block count mismatch");

			Iterator<Expression> itx0 = elseIfCondExprs.iterator();
			Iterator<Block> itx1 = elseIfBlocks.iterator();
			int i = 0;
			while (itx0.hasNext() && itx1.hasNext()) {
			    Expression elseIfExpr = itx0.next();
			    if (elseIfExpr instanceof LiteralValue) {
				LiteralValue lv = (LiteralValue) elseIfExpr;
				if (lv.typeOfValue != TypeDecl.TYPE_BOOLEAN)
				    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					    "emitOpcodesStatement: ifexpr elseif conditional literal not type=BOOLEAN");
				if (lv == LiteralValue.VALUE_TRUE)
				    return emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, itx1.next());
			    } else {
				newblocktag(BlockType.ELSEIF);
				final String exitl = label(String.format("IF_ENDELSEIF_%s", i));
				if (!emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, elseIfExpr))
				    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					    "emitOpcodesStatement: ifexpr elseif conditional not on stack");
				output(new OpcodeControlBitmask(Opcodes.CMP, Opcodes.BITMASK_CMP_CONSUME), elseIfExpr.location);
				output(new OpcodeControlBitmaskLabel(Opcodes.JMP,
					(byte) (Opcodes.BITMASK_JMP_AORO | Opcodes.BITMASK_JMP_JCJF), newlist(-1),
					newstringlist(exitl)));
				if (!emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, itx1.next()))
				    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					    "emitOpcodesStatement: ifexpr elseif-then statements not written");
				output(new OpcodeControlBitmaskLabel(Opcodes.JMP, Opcodes.BITMASK_JMP_AORO, newlist(-1), exitlabel));
				output(exitl);
				endblocktag(BlockType.ELSEIF);
			    }
			    i++;
			}
		    }

		    if (((IfStatement) s).elseBlock != null) {
			newblocktag(BlockType.ELSE);
			if (!emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, ((IfStatement) s).elseBlock))
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: ifexpr else statements not written");
			endblocktag(BlockType.ELSE);
		    }

		    output(exitifl);

		    return true;
		} else if (s instanceof ForStatement) {
		    ForStatement forStmt = (ForStatement) s;

		    newblocktag(BlockType.FOR);
		    newclosure("FOR", forStmt.location);

		    int incrementorSlot;

		    if (forStmt.startLocal != null) {
			int slot = getclosurevar(forStmt.startLocal);
			if (slot == -1) {
			    String label = getglobalvar(linker, forStmt.startLocal);
			    if (label == null)
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: for start local not a local or global");
			    output(new OpcodeControlBitmask(Opcodes.GETGLOBAL, newlist(label)), forStmt.location);
			} else
			    output(new OpcodeControlBitmask(Opcodes.GETLOCAL, newlist(slot)), forStmt.location);
			incrementorSlot = newclosurevar(typeMachine, forStmt.local, TypeDecl.TYPE_INTEGER, forStmt.location);
			output(new OpcodeControlBitmask(Opcodes.SETLOCAL, newlist(incrementorSlot)), forStmt.location);
		    } else if (forStmt.startExpr != null) {
			incrementorSlot = newclosurevar(typeMachine, forStmt.local, TypeDecl.TYPE_INTEGER, forStmt.location);
			emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, forStmt.startExpr);
			output(new OpcodeControlBitmask(Opcodes.SETLOCAL, newlist(incrementorSlot)), forStmt.location);
		    } else
			throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				"emitOpcodesStatement: for start argument not expression or local");

		    String startl = label("FOR_ORIGIN");
		    String endl = label("FOR_EXIT");

		    output(startl);
		    emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, forStmt.thenBlock);

		    if (forStmt.byLocal != null) {
			int slot = getclosurevar(forStmt.byLocal);
			if (slot == -1) {
			    String label = getglobalvar(linker, forStmt.byLocal);
			    if (label == null)
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: for by local not a local or global");
			    output(new OpcodeControlBitmask(Opcodes.GETGLOBAL, newlist(label)), forStmt.location);
			} else
			    output(new OpcodeControlBitmask(Opcodes.GETLOCAL, newlist(slot)), forStmt.location);
		    } else if (forStmt.byExpr != null)
			if (forStmt.byExpr instanceof LiteralValue) {
			    LiteralValue lv = (LiteralValue) forStmt.byExpr;
			    if (lv.typeOfValue == TypeDecl.TYPE_INTEGER)
				output(new OpcodeControlBitmask(Opcodes.PUSHI, newlist(lv.valueObject)), forStmt.location);
			    else if (lv.typeOfValue == TypeDecl.TYPE_REAL)
				output(new OpcodeControlBitmask(Opcodes.PUSHR, newlist(lv.valueObject)), forStmt.location);
			    else
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: for incrementor literal not integer or real type");
			} else
			    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, forStmt.byExpr);
		    else
			throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				"emitOpcodeStatement: for incrementor argument not expression or local");

		    output(new OpcodeControlBitmask(Opcodes.GETLOCAL, newlist(incrementorSlot)));
		    output(new OpcodeControlBitmask(Opcodes.BINOP, newlist(BinaryExpressionParamValueTable.NUM_ADD.value())),
			    forStmt.location);
		    output(Opcodes.DUP, forStmt.location);
		    output(new OpcodeControlBitmask(Opcodes.SETLOCAL, newlist(incrementorSlot)), forStmt.location);

		    if (forStmt.endLocal != null) {
			int slot = getclosurevar(forStmt.endLocal);
			if (slot == -1) {
			    String label = getglobalvar(linker, forStmt.endLocal);
			    if (label == null)
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: for end local not a local or global");
			    output(new OpcodeControlBitmask(Opcodes.GETGLOBAL, newlist(label)), forStmt.location);
			} else
			    output(new OpcodeControlBitmask(Opcodes.GETLOCAL, newlist(slot)), forStmt.location);
		    } else if (forStmt.endExpr != null)
			if (forStmt.endExpr instanceof LiteralValue) {
			    LiteralValue lv = (LiteralValue) forStmt.endExpr;
			    if (lv.typeOfValue == TypeDecl.TYPE_INTEGER)
				output(new OpcodeControlBitmask(Opcodes.PUSHI, newlist(lv.valueObject)), forStmt.location);
			    else if (lv.typeOfValue == TypeDecl.TYPE_REAL)
				output(new OpcodeControlBitmask(Opcodes.PUSHR, newlist(lv.valueObject)), forStmt.location);
			    else
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: for end literal not integer or real type");
			} else
			    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, forStmt.endExpr);
		    else
			throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				"emitOpcodesStatement: for end argument not expression or local");

		    output(new OpcodeControlBitmask(Opcodes.BINOP, newlist(BinaryExpressionParamValueTable.VAL_EQ.value())),
			    forStmt.location);
		    output(new OpcodeControlBitmask(Opcodes.CMP, Opcodes.BITMASK_CMP_CONSUME), forStmt.location);
		    List<String> labelList = new LinkedList<>();
		    labelList.add(startl);
		    output(new OpcodeControlBitmaskLabel(Opcodes.JMP, (byte) (Opcodes.BITMASK_JMP_AORO | Opcodes.BITMASK_JMP_JCJF), newlist(
			    -1), labelList), forStmt.location);
		    output(endl);

		    endclosure(forStmt.location);
		    endblocktag(BlockType.FOR);

		    return true;
		} else if (s instanceof LoopStatement) {
		    newblocktag(BlockType.LOOP);
		    String startLabel = label("LOOP"),
			    exitLabel = label("LOOP_EXIT");
		    m_generatedLoopLabelStack.push(exitLabel);
		    output(startLabel);
		    emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, ((LoopStatement) s).statements);
		    output(new OpcodeControlBitmaskLabel(Opcodes.JMP, Opcodes.BITMASK_JMP_AORO, newlist(-1), newstringlist(startLabel)),
			    ((LoopStatement) s).location);
		    output(exitLabel);
		    if (!m_generatedLoopLabelStack.pop().equals(exitLabel))
			throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				"emitOpcodeStatement: loop label stack mismatch");
		    endblocktag(BlockType.LOOP);
		    return true;
		} else if (s instanceof ExitWhenStatement) {
		    checkblocktag(BlockType.LOOP);
		    if (m_generatedLoopLabelStack.isEmpty())
			throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				"emitOpcodesStatement: exitwhen not within LOOP construct");
		    Expression exitExpr = ((ExitWhenStatement) s).condExitWhenExpr;
		    String jumpLabel = m_generatedLoopLabelStack.peek();
		    if (exitExpr instanceof LiteralValue) {
			LiteralValue lv = (LiteralValue) exitExpr;
			if (lv.typeOfValue != TypeDecl.TYPE_BOOLEAN)
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: exitwhen conditional literal not type=BOOLEAN");
			if (lv.valueObject == LiteralValue.VALUE_TRUE)
			    output(new OpcodeControlBitmaskLabel(Opcodes.JMP, Opcodes.BITMASK_JMP_AORO, newlist(-1),
				    newstringlist(jumpLabel)), ((LiteralValue) exitExpr).location);
		    } else {
			if (!emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, exitExpr))
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: exitwhen conditional not on stack");
			output(new OpcodeControlBitmask(Opcodes.CMP, Opcodes.BITMASK_CMP_CONSUME), exitExpr.location);
			output(new OpcodeControlBitmaskLabel(Opcodes.JMP, (byte) (Opcodes.BITMASK_JMP_AORO | Opcodes.BITMASK_JMP_JCJT),
				newlist(-1), newstringlist(jumpLabel)), ((ExitWhenStatement) s).location);
		    }
		    return true;
		} else if (s instanceof TryCatchStatement) {
		    String label = label(String.format("TRY"));
		    newblocktag(BlockType.TRY);
		    output(new OpcodeControlBitmaskLabel(Opcodes.TRY, (byte) 0, newlist(-1), newstringlist(label)),
			    ((TryCatchStatement) s).location);
		    if (!emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, ((TryCatchStatement) s).tryBlock))
			return false;
		    endblocktag(BlockType.TRY);
		    output(label);

		    Iterator<TryCatchStatement.CatchBlock> itx = ((TryCatchStatement) s).catchBlocks.iterator();
		    int i = 0;
		    while (itx.hasNext()) {
			TryCatchStatement.CatchBlock block = itx.next();
			String catchlabel = label(String.format("CATCH_%s", i++));

			int catchtype = typeMachine.getTypeNumber(block.typeOfCatch);

			newblocktag(BlockType.CATCH);
			if (itx.hasNext())
			    output(new Opcodes.OpcodeControlBitmaskLabel(Opcodes.CATCH, Opcodes.BITMASK_CATCH_HASNEXT,
				    newlist(catchtype, -1), newstringlist(null, catchlabel)), block.location);
			else
			    output(new Opcodes.OpcodeControlBitmaskLabel(Opcodes.CATCH, (byte) 0, newlist(catchtype, -1),
				    newstringlist(null, catchlabel)), block.location);

			newclosure(catchlabel, block.location);
			int ptr = newclosurevar(typeMachine, block.catchLocal, block.typeOfCatch, block.location);
			output(new OpcodeControlBitmask(Opcodes.SETLOCAL, newlist(ptr)), block.location);

			if (!emitOpcodesBlock(typeMachine, linker, declaringChunk, fnDecl, block.block))
			    return false;
			endclosure(block.location);
			endblocktag(BlockType.CATCH);
			output(catchlabel);
		    }

		    return true;
		} else if (s instanceof RaiseStatement) {
		    if (((RaiseStatement) s).raiseValue instanceof LiteralValue) {
			LiteralValue lv = (LiteralValue) ((RaiseStatement) s).raiseValue;
			if (lv.typeOfValue != TypeDecl.TYPE_STRING)
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: literal for RAISE not type=STRING");
			output(new OpcodeControlBitmask(Opcodes.PUSHS, newlist(lv.valueObject)), lv.location);
		    } else
			emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, ((RaiseStatement) s).raiseValue);
		    output(Opcodes.RAISE, ((RaiseStatement) s).location);

		    return true;
		} else if (s instanceof YieldStatement) {
		    YieldStatement yieldStmt = (YieldStatement) s;

		    if (yieldStmt.yieldExpression != null) {
			if (yieldStmt.yieldExpression instanceof LiteralValue) {
			    if (!emitOpcodesLiteral(typeMachine, declaringChunk, fnDecl, inBlock, (LiteralValue) yieldStmt.yieldExpression))
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: literal not PUSHed on stack");
			} else if (!emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, yieldStmt.yieldExpression))
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: yield yieldexpr not on stack");
			output(new OpcodeControlBitmask(Opcodes.YIELD, (byte) Opcodes.BITMASK_YIELD_STACK), yieldStmt.location);
		    } else
			output(Opcodes.YIELD, yieldStmt.location);

		    return true;
		} else if (s instanceof ReturnStatement) {
		    ReturnStatement returnStmt = (ReturnStatement) s;
		    if (returnStmt.returnExpression != null)
			if (returnStmt.returnExpression instanceof LiteralValue) {
			    if (!emitOpcodesLiteral(typeMachine, declaringChunk, fnDecl, inBlock, (LiteralValue) returnStmt.returnExpression))
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: literal not PUSHed on stack");
			    output(new OpcodeControlBitmask(Opcodes.RETN, Opcodes.BITMASK_RETN_RVAL), returnStmt.location);
			} else {
			    if (!emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, returnStmt.returnExpression))
				throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
					"emitOpcodesStatement: return returnexpr not on stack");
			    output(new OpcodeControlBitmask(Opcodes.RETN, Opcodes.BITMASK_RETN_RVAL), returnStmt.location);
			}
		    else
			output(new OpcodeControlBitmask(Opcodes.RETN, (byte) 0), returnStmt.location);
		    return true;
		}
	    } else if (s instanceof ExecutionFunctionStatement) {
		if (s instanceof DebugStatement) {
		    DebugStatement debugStmt = (DebugStatement) s;
		    if (debugStmt.statement != null) {
			output(Opcodes.DEBUG, debugStmt.location);
			if (!emitOpcodesStatement(typeMachine, linker, declaringChunk, fnDecl, inBlock, debugStmt.statement))
			    throw new FunctionAssemblySourceException(declaringChunk, fnDecl, s,
				    "emitOpcodesStatement: statement following DEBUG not written");
			return true;
		    }
		} else if (s instanceof FunctionCallStatement) {
		    FunctionCallStatement fcs = (FunctionCallStatement) s;
		    if (fcs.params != null && fcs.params.size() != 0) {
			Iterator<Expression> itx = fcs.params.iterator();
			while (itx.hasNext())
			    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, itx.next());
		    }
		    output(new OpcodeControlBitmask(Opcodes.CALL, newlist(fcs.nameOfFunction, (fcs.params != null) ? fcs.params.size() : 0)),
			    fcs.location);
		    return true;
		} else if (s instanceof FunctionCallVarStatement) {
		    FunctionCallVarStatement fcvs = (FunctionCallVarStatement) s;
		    if (fcvs.params != null && fcvs.params.size() != 0) {
			Iterator<Expression> itx = fcvs.params.iterator();
			while (itx.hasNext())
			    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, itx.next());
		    }
		    int slot = getclosurevar(fcvs.nameOfLocalOrGlobalVar);
		    if (slot != -1)
			output(new OpcodeControlBitmask(Opcodes.CALLLOCAL, newlist(slot, (fcvs.params != null) ? fcvs.params.size() : 0)),
				fcvs.location);
		    else
			output(new OpcodeControlBitmask(Opcodes.CALLGLOBAL, newlist(fcvs.nameOfLocalOrGlobalVar,
				(fcvs.params != null) ? fcvs.params.size() : 0)), fcvs.location);
		    return true;
		} else if (s instanceof SetVariableStatement) {
		    SetVariableStatement sv = (SetVariableStatement) s;
		    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, sv.rhs);
		    int slot = getclosurevar(sv.nameOfLocalVar);
		    if (slot != -1)
			output(new OpcodeControlBitmask(Opcodes.SETLOCAL, newlist(slot)), sv.location);
		    else
			output(new OpcodeControlBitmask(Opcodes.SETGLOBAL, newlist(sv.nameOfLocalVar)), sv.location);
		    return true;
		} else if (s instanceof SetArrayVariableStatement) {
		    SetArrayVariableStatement sav = (SetArrayVariableStatement) s;
		    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, sav.rhs);
		    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, sav.indexOfArrayExpr);
		    int slot = getclosurevar(sav.nameOfLocalVar);
		    if (slot != -1)
			output(new OpcodeControlBitmask(Opcodes.SETLOCALARR, newlist(slot)), sav.location);
		    else
			output(new OpcodeControlBitmask(Opcodes.SETGLOBALARR, newlist(sav.nameOfLocalVar)), sav.location);
		    return true;
		}
	    } else if (s instanceof DirectAssembly) {
		DirectAssembly direct = (DirectAssembly) s;
		Iterator<Opcode> itx = direct.opcodes.iterator();
		while (itx.hasNext())
		    output(itx.next(), direct.location);
		return true;
	    }
	    return false;
	}

	/**
	 * Emit component(s) of an Expression to bytecode.The AST expression tree is read left-recursively; the AST tree therefore contains
	 * the right-most term(s) of the binary expression (if the top level expression is a binary expression), or begins with a single
	 * unary operation followed by any expression, or is a literal value which may directly be placed on the stack.
	 *
	 * @param typeMachine    The type assembler
	 * @param linker         The linker
	 * @param declaringChunk The declaring code chunk
	 * @param fnDecl         The function declaration
	 * @param inBlock        The function declaration block
	 * @param e              The expression to be written to bytecode
	 *
	 * @return If the Expression was written to bytecode or not
	 *
	 * @throws java.lang.InstantiationException
	 * @throws logicscript.assembler.writer.FunctionAssembler.FunctionAssemblySourceException
	 */
	boolean emitOpcodesExpression(TypeAssembler typeMachine, LinkerObjectFile linker, Chunk declaringChunk, FunctionDecl fnDecl,
		Block inBlock, Expression e) throws IllegalStateException, InstantiationException, FunctionAssemblySourceException {

	    if (e instanceof ParenExpression)
		return emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, ((ParenExpression) e).expressionInParenth);
	    else if (e instanceof BinaryExpression) {
		emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, ((BinaryExpression) e).lhs);
		emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, ((BinaryExpression) e).rhs);
		List<Object> paramList = new LinkedList<>();
		paramList.add(ExpressionParamConverter.fromASTBinaryToBytecodeBinary(((BinaryExpression) e).operand).value());
		output(new OpcodeControlBitmask(Opcodes.BINOP, paramList), ((BinaryExpression) e).location);
		return true;
	    } else if (e instanceof UnaryExpression) {
		emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, ((UnaryExpression) e).rhs);
		List<Object> paramList = new LinkedList<>();
		paramList.add(ExpressionParamConverter.fromASTUnaryToBytecodeUnary(((UnaryExpression) e).operand).value());
		output(new OpcodeControlBitmask(Opcodes.UNOP, paramList), ((UnaryExpression) e).location);
		return true;
	    } else if (e instanceof FunctionCallExpression) {
		/* function call by fnname */
		if (((FunctionCallExpression) e).params != null) {
		    Iterator<Expression> paramExprs = ((FunctionCallExpression) e).params.iterator();
		    while (paramExprs.hasNext())
			emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, paramExprs.next());
		}
		List<Object> paramList = new LinkedList<>();
		paramList.add(((FunctionCallExpression) e).nameOfFunction);
		paramList.add((((FunctionCallExpression) e).params != null) ? ((FunctionCallExpression) e).params.size() : 0);
		output(new OpcodeControlBitmask(Opcodes.CALL, Opcodes.BITMASK_CALL_RVAL, paramList), ((FunctionCallExpression) e).location);
		return true;
	    } else if (e instanceof FunctionCallVarExpression) {
		/* function call by pointer deref */
		if (((FunctionCallVarExpression) e).params != null) {
		    Iterator<Expression> paramExprs = ((FunctionCallVarExpression) e).params.iterator();
		    while (paramExprs.hasNext())
			emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, paramExprs.next());
		}

		int slot = getclosurevar(((FunctionCallVarExpression) e).nameOfLocalOrGlobalVar);
		if (slot != -1)
		    output(new OpcodeControlBitmask(Opcodes.CALLLOCAL, Opcodes.BITMASK_CALLLOCAL_RVAL, newlist(slot)),
			    ((FunctionCallVarExpression) e).location);
		else
		    output(new OpcodeControlBitmask(Opcodes.CALLGLOBAL, Opcodes.BITMASK_CALLGLOBAL_RVAL,
			    newlist(((FunctionCallVarExpression) e).nameOfLocalOrGlobalVar)), ((FunctionCallVarExpression) e).location);
		return true;
	    } else if (e instanceof VariableArrayRefExpression) {
		/* variable array index get */
		emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock,
			((VariableArrayRefExpression) e).indexOfArrayExpr);
		int slot = getclosurevar(((VariableArrayRefExpression) e).nameOfLocalOrGlobalVar);
		if (slot != -1)
		    output(new OpcodeControlBitmask(Opcodes.GETLOCALARR, newlist(slot)), ((VariableArrayRefExpression) e).location);
		else {
		    String label = getglobalvar(linker, ((VariableArrayRefExpression) e).nameOfLocalOrGlobalVar);
		    if (label == null)
			throw new FunctionAssemblySourceException(declaringChunk, fnDecl, e,
				"emitOpcodesExpression: variable not a local or global var");
		    output(new OpcodeControlBitmask(Opcodes.GETGLOBALARR, newlist(label)),
			    ((VariableArrayRefExpression) e).location);
		}
		return true;
	    } else if (e instanceof VariableRefExpression) {
		/* variable literal value get */
		int slot = getclosurevar(((VariableRefExpression) e).nameOfLocalOrGlobalVar);
		if (slot != -1)
		    output(new OpcodeControlBitmask(Opcodes.GETLOCAL, newlist(slot)), ((VariableRefExpression) e).location);
		else {
		    String label = getglobalvar(linker, ((VariableRefExpression) e).nameOfLocalOrGlobalVar);
		    if (label == null)
			throw new FunctionAssemblySourceException(declaringChunk, fnDecl, e,
				"emitOpcodesExpression: variable not a local or global var");
		    output(new OpcodeControlBitmask(Opcodes.GETGLOBAL, newlist(label)),
			    ((VariableRefExpression) e).location);
		}
		return true;
	    } else if (e instanceof FunctionRefExpression) {
		/* function pointer creation */
		output(new OpcodeControlBitmask(Opcodes.FUNCPTR, newlist(((FunctionRefExpression) e).nameOfFunction)),
			((FunctionRefExpression) e).location);
		return true;
	    } else if (e instanceof LiteralValue)
		/* it's a `?`: */
		return emitOpcodesLiteral(typeMachine, declaringChunk, fnDecl, inBlock, (LiteralValue) e);
	    return false;
	}

	boolean emitOpcodesLiteral(TypeAssembler typeMachine, Chunk declChunk, FunctionDecl fnDecl, Block inBlock, LiteralValue lv)
		throws IllegalStateException, InstantiationException, FunctionAssemblySourceException {
	    if (lv.typeOfValue == TypeDecl.TYPE_BOOLEAN)
		output(new OpcodeControlBitmask(Opcodes.PUSHB, newlist(lv == LiteralValue.VALUE_TRUE)), lv.location);
	    else if (lv.typeOfValue == TypeDecl.TYPE_INTEGER)
		output(new OpcodeControlBitmask(Opcodes.PUSHI, newlist(lv.valueObject)), lv.location);
	    else if (lv.typeOfValue == TypeDecl.TYPE_REAL)
		output(new OpcodeControlBitmask(Opcodes.PUSHR, newlist(lv.valueObject)), lv.location);
	    else if (lv.typeOfValue == TypeDecl.TYPE_STRING)
		output(new OpcodeControlBitmask(Opcodes.PUSHS, newlist(lv.valueObject)), lv.location);
	    else
		throw new FunctionAssemblySourceException(declChunk, fnDecl, lv,
			"emitOpcodesLiteral: unexpected literal type for RETURN statement");
	    return true;
	}

	/**
	 * Allocates a closure and places it on the function closure stack, computing the next local variable slot. If the function is a
	 * closure, then the starting slot shall be at least the next slot after the function closure size. If there are closures already on
	 * the stack for the function, then the starting slot shall be the next slot after the last closure on the stack.
	 *
	 * @param label    The debug label for the closure
	 * @param location The debug location which created the closure
	 */
	void newclosure(String label, Location location) {
	    GeneratorClosure gc = new GeneratorClosure();
	    int slot = 0;
	    // start at the slot after the function closure decls:
	    if (m_generatedFunctionClosure != null)
		slot = m_generatedFunctionClosure.start + m_generatedFunctionClosure.count;
	    // if there is already some closure on the stack, which may use the lvars after...
	    if (!m_generatedClosures.isEmpty()) {
		// .. get the last (top) stack item, which will be the farthest to the right
		// for allocation of lvar(s)....
		GeneratorClosure lastClosureOnStack = m_generatedClosures.peek();
		// ... then compute the next free address *after* the closure before; which shall
		// have computed the next free address from the closure before, and so on....
		slot = lastClosureOnStack.start + lastClosureOnStack.count;
	    }
	    gc.label = label;
	    gc.start = slot;

	    gc.names = new String[ s_maxFuncSubClosureVars ];
	    gc.types = new int[ s_maxFuncSubClosureVars ];
	    gc.arrays = new boolean[ s_maxFuncSubClosureVars ];

	    m_generatedClosures.push(gc);
	    output(Opcodes.INCLOSURE, location);
	}

	/**
	 * Generates a new closure-local variable for the current closure on the stack.
	 *
	 * @param typeMachine The type-name assembler machine reference
	 * @param nameOfLocal The name of the local closure variable; must be distinct from the current closure variables
	 * @param typeOfLocal The type declaration type of the closure variable
	 * @param location    The debug location which created the closure-local variable
	 *
	 * @return The local slot which was allocated
	 *
	 * @throws IllegalStateException                                                          Any exception which occurs because the
	 *                                                                                        closure variable cannot be allocated, such
	 *                                                                                        as a duplicate closure variable name or
	 *                                                                                        exhaustion of available closure variable
	 *                                                                                        slots
	 * @throws java.lang.InstantiationException
	 * @throws logicscript.assembler.writer.TypeAssembler.TypeAssemblerLookupException
	 * @throws logicscript.assembler.writer.FunctionAssembler.FunctionAssemblySourceException
	 */
	int newclosurevar(TypeAssembler typeMachine, String nameOfLocal, TypeDecl typeOfLocal, Location location)
		throws IllegalStateException, InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	    if (m_generatedClosures.isEmpty())
		throw new IllegalStateException("newclosurevar: cannot newupvar when not inclosure scope");
	    GeneratorClosure closure = m_generatedClosures.peek();
	    if (!closure.checkname_distinct(nameOfLocal))
		throw new IllegalStateException("newclosurevar: duplicate closure var name");
	    int numberOfTypeOfLocal = typeMachine.getTypeNumber(typeOfLocal);

	    int ptr = closure.count;
	    if (ptr >= s_maxFuncSubClosureVars)
		throw new IllegalStateException("newclosurevar: too many closure variables");
	    ++closure.count;
	    closure.names[ptr] = nameOfLocal;
	    closure.types[ptr] = numberOfTypeOfLocal;
	    closure.arrays[ptr] = false;

	    output(new OpcodeControlBitmask(Opcodes.NEWUPVAR, newlist(numberOfTypeOfLocal, closure.start + ptr)), location);
	    return closure.start + ptr;
	}

	/**
	 * Generates a new closure-local array variable for the current closure on the stack.
	 *
	 * @param typeMachine    The type-name assembler machine reference
	 * @param linker         The linker destination struct
	 * @param declaringChunk The AST chunk which defined the FunctionDecl
	 * @param fnDecl         The FunctionDecl being compiled
	 * @param inBlock        The Block in the FunctionDecl being compiled
	 * @param nameOfLocal    The name of the local array variable to allocate
	 * @param typeOfLocal    The type of the local array variable to allocate
	 * @param dimExpr        The expression which defines the dimension of the array
	 * @param location	      The debug location which created the closure-local array variable
	 *
	 * @return The local slot which was allocated
	 *
	 * @throws IllegalStateException                                                          Any exception which occurs because the
	 *                                                                                        closure variable cannot be allocated, such
	 *                                                                                        as a duplicate closure variable name or
	 *                                                                                        exhaustion of available closure variable
	 *                                                                                        slots
	 * @throws java.lang.InstantiationException
	 * @throws logicscript.assembler.writer.TypeAssembler.TypeAssemblerLookupException
	 * @throws logicscript.assembler.writer.FunctionAssembler.FunctionAssemblySourceException
	 */
	int newclosurevararray(TypeAssembler typeMachine, LinkerObjectFile linker, Chunk declaringChunk, FunctionDecl fnDecl, Block inBlock,
		String nameOfLocal, TypeDecl typeOfLocal, Expression dimExpr, Location location)
		throws IllegalStateException, InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	    if (m_generatedClosures.isEmpty())
		throw new IllegalStateException("newclosurevararray: cannot newclosurevararray when not inclosure scope");
	    GeneratorClosure closure = m_generatedClosures.peek();
	    if (!closure.checkname_distinct(nameOfLocal))
		throw new IllegalStateException("newclosurevararray: duplicate closure var name");
	    int numberOfTypeOfLocal = typeMachine.getTypeNumber(typeOfLocal);

	    int ptr = closure.count;
	    if (ptr >= s_maxFuncSubClosureVars)
		throw new IllegalStateException("newclosurevararray: too many closure variables");
	    ++closure.count;
	    closure.names[ptr] = nameOfLocal;
	    closure.types[ptr] = numberOfTypeOfLocal;
	    closure.arrays[ptr] = true;

	    // generate assembly object:
	    emitOpcodesExpression(typeMachine, linker, declaringChunk, fnDecl, inBlock, dimExpr);
	    output(new OpcodeControlBitmask(Opcodes.NEWUPVAR, Opcodes.BITMASK_NEWUPVAR_ARRAY, newlist(ptr, numberOfTypeOfLocal)), location);
	    return ptr;
	}

	int getclosurevar(String nameOfLocal) throws FunctionAssemblySourceException {
	    int slot = -1;
	    if (!m_generatedClosures.isEmpty()) {
		Iterator< GeneratorClosure> itx = m_generatedClosures.iterator();
		while (itx.hasNext()) {
		    GeneratorClosure closureObj = itx.next();
		    slot = closureObj.slot(nameOfLocal);
		    if (slot != -1)
			return (closureObj.start + slot);
		}
	    }

	    if (m_generatedFunctionClosure != null)
		slot = m_generatedFunctionClosure.slot(nameOfLocal);
	    return slot;
	}

	String getglobalvar(LinkerObjectFile linker, String nameOfGlobalUnsafe) throws FunctionAssemblySourceException {
	    Iterator<GlobalsEntry> itx0 = linker.globalsTable.iterator();
	    while (itx0.hasNext()) {
		GlobalsEntry g = itx0.next();
		if (g.name.equals(nameOfGlobalUnsafe))
		    return g.name;
	    }

	    Iterator<ImportsEntry> itx1 = linker.importsTable.iterator();
	    while (itx1.hasNext()) {
		ImportsEntry e = itx1.next();
		if ((e.typeflags & LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL) != 0) {
		    Iterator<String> itx2 = e.labels.iterator();
		    while (itx2.hasNext()) {
			String name = itx2.next();
			if (name.equals(nameOfGlobalUnsafe))
			    return name;
		    }
		}
	    }
	    return null;
	}

	/**
	 * Ends the current closure on the stack.
	 *
	 * @param location The debug location which ended the current closure on the stack
	 *
	 * @throws IllegalStateException                                                          If the closure stack is empty, an
	 *                                                                                        IllegalStateException is thrown.
	 * @throws logicscript.assembler.writer.FunctionAssembler.FunctionAssemblySourceException
	 */
	void endclosure(Location location) throws IllegalStateException, FunctionAssemblySourceException {
	    try {
		m_generatedClosures.pop();
	    } catch (EmptyStackException ese) {
		throw new IllegalStateException("endclosure: cannot endclosure when not inclosure scope");
	    }
	    output(Opcodes.ENDCLOSURE, location);
	}

	/**
	 * @see #newblocktag(logicscript.assembler.writer.FunctionAssembler.FunctionAssemblyGeneratorMachine.BlockType, java.lang.String)
	 * @param typeof The type of the block tag
	 */
	void newblocktag(BlockType typeof) {
	    newblocktag(typeof, null);
	}

	/**
	 * Generates an internal block-level tag and places it on the block-tag stack. The type of the block must be specified; an optional
	 * label may be used for debugging symbol generation.
	 *
	 * @param typeof The type of the block tag
	 * @param label  The label for the block tag, or null if no label is desired
	 */
	void newblocktag(BlockType typeof, String label) {
	    GeneratorBlock asFinalType = null;
	    if (label == null || label.length() == 0)
		asFinalType = typeof; // no typebox
	    else
		asFinalType = new BlockTypeLabel(typeof, label); // like pair<a,b> but... not?
	    m_generatedBlockStack.push(asFinalType);
	}

	/**
	 * Check that the stack contains at least one of specified tag type on the stack.
	 *
	 * @param typeof The type of the block tag
	 *
	 * @return If the block-tag stack contains at least one of the specified tag on the stack.
	 */
	boolean checkblocktag(BlockType typeof) {
	    return (blocktagdepth(typeof) != -1);
	}

	/**
	 * Get the depth of the first block-tag of the type specified on the block-tag stack.
	 *
	 * @param typeof The type of the block tag
	 *
	 * @return The index of the first instance of the block-tag on the stack, or <code>-1</code> if the tag is not on the stack.
	 */
	int blocktagdepth(BlockType typeof) {
	    Iterator< GeneratorBlock> itx = m_generatedBlockStack.iterator();
	    int depth = 0;
	    while (itx.hasNext()) {
		GeneratorBlock typeHintOnStack = itx.next();
		if (typeHintOnStack.ordinal() == typeof.ordinal())
		    return depth;
		depth++;
	    }
	    return -1;
	}

	/**
	 * Ends a block-level tag from the block-tag stack. The type of the block must be specified.
	 *
	 * @param typeof The type of the block-tag to remove from the stack.
	 *
	 * @throws IllegalStateException If the type of the block tag removed from the stack does not match the provided block type, then an
	 *                               IllegalStateException is thrown.
	 */
	void endblocktag(BlockType typeof) {
	    GeneratorBlock stackNode = m_generatedBlockStack.pop();
	    if (stackNode.ordinal() != typeof.ordinal())
		throw new IllegalStateException("endblocktag: stack mismatch");
	}

	/**
	 * Generate a unique location label.
	 *
	 * @param suffix The suffix for the label.
	 *
	 * @return The generated label string.
	 */
	String label(String suffix) {
	    StringBuilder sb = new StringBuilder();
	    sb.append(m_generatedObjectList.size()).append(":");
	    Iterator< GeneratorClosure> itx = m_generatedClosures.iterator();
	    while (itx.hasNext())
		sb.append(itx.next().label).append("/");
	    return (sb.append(suffix)).toString();
	}

	List<Object> newlist(Object... theElementsAsVariadic) {
	    LinkedList<Object> alloc = new LinkedList<>();
	    for (int i = 0, j = theElementsAsVariadic.length; i < j; i++)
		alloc.add(theElementsAsVariadic[i]);
	    return alloc;
	}

	List<String> newstringlist(String... theStringsAsVariadic) {
	    LinkedList<String> alloc = new LinkedList<>();
	    for (int i = 0, j = theStringsAsVariadic.length; i < j; i++)
		alloc.add(theStringsAsVariadic[i]);
	    return alloc;
	}

	void output(Opcode opcode) {
	    m_generatedObjectList.add(new Pair<>(opcode, null));
	}

	void output(Opcode opcode, int line, int col) {
	    m_generatedObjectList.add(new Pair<>(opcode, new DebugLocationSegment(line, col)));
	}

	void output(Opcode opcode, Location loc) {
	    if (loc != null)
		output(opcode, loc.beginLine, loc.beginCol);
	    else
		output(opcode);
	}

	void output(String label) {
	    m_generatedObjectList.add(new Pair<>(label, null));
	}

	boolean stream(LogicBytecodeFile dst) throws IllegalStateException {
	    if (m_isAssembling)
		throw new IllegalStateException("still assembling");
	    if (!m_hasAssembled)
		throw new IllegalStateException("not assembled");
	    return dst.add(m_generatingFunctionObj);
	}

    }

    /**
     * The function generator machine instance.
     */
    final FunctionAssemblyGeneratorMachine fnGenerator = new FunctionAssemblyGeneratorMachine();

    /**
     * The bidirectional Map of all function declarations which have been compiled, AST declaration.
     */
    final BiDiMap<String, FunctionDecl> compiledFunctionTable = new BiDiMap<>();

    /**
     * If the function assembler should generate debugging symbols or not.
     */
    volatile boolean m_generateDebugSymbols = false;

    public FunctionAssembler() {
	reset();
    }

    public FunctionAssembler addFunctionAssemblyTransformer(FunctionBytecodeTransformer t) {
	this.fnGenerator.addTransformer(t);
	return this;
    }

    /**
     * Set if the function assembler should generate debugging symbols attached to the output bytecode.
     *
     * @param b If debug symbols should be generated.
     *
     * @return The function assembler instance.
     */
    public FunctionAssembler setShouldGenerateDebugSymbols(boolean b) {
	this.m_generateDebugSymbols = b;
	return this;
    }

    /**
     * Reset the function assembler. The list of compiled functions is cleared so that another Chunk may be compiled without reallocating
     * the function assembler.
     */
    public void reset() {
	this.compiledFunctionTable.clear();
    }

    /**
     * Compile a user function.
     *
     * @param dstLinkObj	         The linker object which contains the compiled import/export table
     * @param dstFileObj          Destination buffer
     * @param typeCompilerMachine The type compiler machine, for type compilation referencing
     * @param declaringChunk      The code chunk which declares the function
     * @param funcDecl            The function declaration object
     *
     * @throws java.lang.InstantiationException
     * @throws logicscript.assembler.writer.TypeAssembler.TypeAssemblerLookupException
     *
     * @throws FunctionAssemblySourceException                                         Any compilation-level exception
     */
    public void assembleUserFunction(LinkerObjectFile dstLinkObj, LogicBytecodeFile dstFileObj, TypeAssembler typeCompilerMachine,
	    Chunk declaringChunk, FunctionDecl funcDecl) throws InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	if (fnGenerator.
		reset().
		initShouldDebug(m_generateDebugSymbols).
		initUserFn().
		emit(typeCompilerMachine, dstLinkObj, declaringChunk, funcDecl)) {
	    fnGenerator.stream(dstFileObj);
	    compiledFunctionTable.put(funcDecl.name, funcDecl);
	} else
	    throw new FunctionAssemblySourceException(declaringChunk, funcDecl, "no bytecode generated");
    }

    /**
     * Compile a native function.
     *
     * @param dstLinkObj          The linker object which contains the compiled import/export table
     * @param dstFileObj          Destination buffer
     * @param typeCompilerMachine The type compiler machine, for type compilation referencing
     * @param declaringChunk      The code chunk which declares the function
     * @param funcDecl            The native function declaration object
     *
     * @throws java.lang.InstantiationException
     * @throws logicscript.assembler.writer.TypeAssembler.TypeAssemblerLookupException
     *
     * @throws FunctionAssemblySourceException                                         Any compilation-level exception
     */
    public void assembleNativeFunction(LinkerObjectFile dstLinkObj, LogicBytecodeFile dstFileObj, TypeAssembler typeCompilerMachine,
	    Chunk declaringChunk, FunctionDecl funcDecl) throws InstantiationException, TypeAssemblerLookupException, FunctionAssemblySourceException {
	if (fnGenerator.
		reset().
		initShouldDebug(m_generateDebugSymbols).
		initNativeFn().
		emit(typeCompilerMachine, dstLinkObj, declaringChunk, funcDecl)) {
	    fnGenerator.stream(dstFileObj);
	    compiledFunctionTable.put(funcDecl.name, funcDecl);
	} else
	    throw new FunctionAssemblySourceException(declaringChunk, funcDecl, "no bytecode generated");
    }

    /**
     * Compile a function export statement.
     *
     * @param dstLinkObj          The destination linker buffer
     * @param typeCompilerMachine The type compiler machine, for type compilation referencing
     * @param declaringChunk      The code chunk which declares the export statement
     * @param exportBlock         The block within the export statement
     * @param referentFuncs       The referent function list
     *
     * @throws FunctionAssemblySourceException
     */
    public void assembleFunctionExport(LinkerObjectFile dstLinkObj, TypeAssembler typeCompilerMachine, Chunk declaringChunk,
	    LibraryBlock exportBlock, LibraryReference referentFuncs) throws FunctionAssemblySourceException {
	ExportsEntry asCompiled = new ExportsEntry();
	asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_EXPORTS_FUNCTION;
	asCompiled.name = exportBlock.symbol;
	asCompiled.labels = referentFuncs.tokens;
	dstLinkObj.exportsTable.add(asCompiled);
    }

    /**
     * Compile a function import statement.
     *
     * @param dstLinkObj          The destination linker buffer
     * @param typeCompilerMachine The type compiler machine, for type compilation referencing
     * @param declaringChunk      The code chunk which declares the export statement
     * @param importBlock         The block within the import statement
     * @param referentFuncs       The referent function list
     *
     * @throws FunctionAssemblySourceException
     */
    public void assembleFunctionImport(LinkerObjectFile dstLinkObj, TypeAssembler typeCompilerMachine, Chunk declaringChunk,
	    LibraryBlock importBlock, LibraryReference referentFuncs) throws FunctionAssemblySourceException {
	if (referentFuncs.isWildcard) {
	    ImportsEntry asCompiled = new ImportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_IMPORTS_FUNCTION | LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD;
	    asCompiled.name = importBlock.symbol;
	    dstLinkObj.importsTable.add(asCompiled);
	} else {
	    ImportsEntry asCompiled = new ImportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_IMPORTS_FUNCTION;
	    asCompiled.name = importBlock.symbol;
	    asCompiled.labels = referentFuncs.tokens;
	    dstLinkObj.importsTable.add(asCompiled);
	}
    }

}
