package logicscript.assembler.writer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicbyte.bytecode.Opcode;
import logicbyte.bytecode.Opcode.OpcodeControlBitmask;
import logicbyte.bytecode.Opcode.Opcodes;
import logicbyte.bytecode.validate.LogicBytecodeValidator;
import logicscript.assembler.FunctionBytecodeTransformer;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionDecl.FunctionDeclType;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.DirectAssembly;
import logicscript.ast.nodes.stmt.ef.SetVariableStatement;
import logictype.Log;

/**
 * <p>
 * Main chunk assembler; assembles AST root-level Chunk (parsed source) node(s) into assembled linker (.o) and bytecode (.lb) file(s) for
 * usage in the VM.</p>
 *
 *
 * @author Lochie
 */
public class ChunkAssembler {

    /**
     * The type assembler instance
     */
    final TypeAssembler typeMachine = new TypeAssembler();
    /**
     * The function assembler instance
     */
    final FunctionAssembler functionMachine = new FunctionAssembler();
    /**
     * The globals assembler instance
     */
    final GlobalsAssembler globalsMachine = new GlobalsAssembler();

    /**
     * The validator instance
     */
    final LogicBytecodeValidator validator = new LogicBytecodeValidator();

    volatile Log logger;

    public ChunkAssembler() {
    }

    /**
     * Set if the chunk assembler should generate debug symbols with applicable debug-able objects, such as function bodies.
     *
     * @param b If debug symbols should be included in output linker / bytecode file(s).
     */
    public void setShouldGenerateDebugSymbols(boolean b) {
	this.functionMachine.setShouldGenerateDebugSymbols(b);
    }
    
    public void addFunctionAssemblyTransformer(FunctionBytecodeTransformer impl) {
	this.functionMachine.addFunctionAssemblyTransformer(impl);
    }

    public void setLog(Log l) {
	this.logger = l;
    }

    /**
     * Reset the chunk assembler so that a new chunk can be assembled without reallocation of the assembler.
     */
    public void reset() {
	typeMachine.reset();
	functionMachine.reset();
	globalsMachine.reset();
	validator.reset();
    }

    /**
     * <p>
     * Assemble a Chunk and place the resultant struct(s) in the provided destination linker and destination bytecode object(s), which must
     * be allocated externally.For an AST Chunk, the following occurs:</p>
     *
     * <ul>
     * <li>The IMPORTS statement(s), if any, are compiled so that external types, globals and functions are known;</li>
     * <li>The GLOBALS statement(s), if any, are compiled so that the name and the type of locally-defined globals are known;</li>
     * <li>The FUNCTION declaration(s), if any, are compiled so that local functions are fully formalized;</li>
     * <li>The GLOBALS statement(s), if any, have their initialization expressions placed in the <code>&lt;ginit&gt;</code> metafunction;
     * then</li>
     * <li>The EXPORTS statement(s), if any, are compiled so that exposed types, globals and functions are discoverable.</li>
     * </ul>
     *
     * @param codeChunk        The AST root-node to assemble
     * @param dstLinkObj       The destination linker object
     * @param dstFnAssembleObj The destination bytecode object
     * @param logInst          The logging instance, or the local global logger (optional)
     *
     * @throws Exception Any checked exception which occurs during assembly of the AST
     */
    public void assembleChunk(
	    Chunk codeChunk,
	    LinkerObjectFile dstLinkObj,
	    LogicBytecodeFile dstFnAssembleObj,
	    Log logInst) throws Exception {
	/*  
	    assemble linker information first; this ensures the state machines 
	    are in the correct state for subsequent bytecode compilation:
	 */
	final Log destLog = (logInst != null) ? logInst : ((logger != null) ? logger : Log.VoidLog.INSTANCE);
	assembleChunkLinkerImportData(dstLinkObj, codeChunk, destLog);
	assembleChunkGlobalBytecodeData(dstLinkObj, dstFnAssembleObj, codeChunk, destLog);
	assembleChunkBytecodeData(dstLinkObj, dstFnAssembleObj, codeChunk, destLog);
	assembleChunkLinkerExportData(dstLinkObj, codeChunk, destLog);
	validator.validate(dstLinkObj, dstFnAssembleObj, destLog);
    }

    /**
     * Internal: Assembles IMPORTS symbols.
     *
     * @param dstLinkObj The destination linker object
     * @param codeChunk  The AST root node
     * @param log        The logger or <code>null</code>
     *
     * @throws Exception
     */
    void assembleChunkLinkerImportData(LinkerObjectFile dstLinkObj, Chunk codeChunk, Log log) throws Exception {
	log.debug("assembling chunk import data");
	if (codeChunk.libImportsDecls != null && !codeChunk.libImportsDecls.isEmpty()) {
	    Iterator<LibraryBlock> itx = codeChunk.libImportsDecls.iterator();
	    while (itx.hasNext()) {
		LibraryBlock importDecl = itx.next();
		log.trace("assembling import data for symbol `%s`", importDecl.symbol);
		Iterator<LibraryReference> itx1 = importDecl.iterator();
		while (itx1.hasNext()) {
		    LibraryReference referentSymbolSet = itx1.next();
		    log.trace("assembling import data for symbol `%s`, type %s", importDecl.symbol, referentSymbolSet.type);
		    switch (referentSymbolSet.type) {
			case TYPE:
			    typeMachine.assembleTypeImport(dstLinkObj, codeChunk, importDecl, referentSymbolSet, log);
			    break;
			case GLOBAL:
			    globalsMachine.assembleGlobalImport(dstLinkObj, typeMachine, codeChunk, importDecl, referentSymbolSet, log);
			    break;
			case FUNCTION:
			    functionMachine.assembleFunctionImport(dstLinkObj, typeMachine, codeChunk, importDecl, referentSymbolSet, log);
			    break;
			default:
			    throw new Exception("unsupported referent symbol type");
		    }
		}
	    }
	}

	if (codeChunk.utypeDecls != null && !codeChunk.utypeDecls.isEmpty()) {
	    Iterator<TypeDecl> itx = codeChunk.utypeDecls.iterator();
	    while (itx.hasNext())
		typeMachine.assembleTypeDecl(dstLinkObj, codeChunk, itx.next(), log);
	}

	if (codeChunk.globalsDecls != null && !codeChunk.globalsDecls.isEmpty()) {
	    Iterator<GlobalsBlock> itx = codeChunk.globalsDecls.iterator();
	    while (itx.hasNext()) {
		GlobalsBlock block = itx.next();
		Iterator<GlobalsVarDecl> itx1 = block.iterator();
		while (itx1.hasNext())
		    globalsMachine.assembleGlobalDeclaration(dstLinkObj, typeMachine, codeChunk, block, itx1.next(), log);
	    }
	}
    }

    /**
     * Internal: Assembles GLOBALS declarations.
     *
     * @param dstLinkObj       The destination linker object
     * @param dstFnAssembleObj The destination bytecode object
     * @param codeChunk        The root AST node
     * @param log              The logger or <code>null</code>
     *
     * @throws Exception
     */
    void assembleChunkGlobalBytecodeData(LinkerObjectFile dstLinkObj, LogicBytecodeFile dstFnAssembleObj, Chunk codeChunk, Log log) throws Exception {
	log.debug("assembling chunk globals");
	if (codeChunk.globalsDecls != null && !codeChunk.globalsDecls.isEmpty()) {
	    log.trace("generating globals init function...");
	    FunctionDecl globalInitFn = null;
	    
	    if ( codeChunk.ufuncDecls == null ) 
		codeChunk.ufuncDecls = new LinkedList<>();
	    
	    Iterator<FunctionDecl> itx = codeChunk.ufuncDecls.iterator();
	    while (itx.hasNext()) {
		FunctionDecl decl = itx.next();
		if (decl.name.equals("<ginit>")) {
		    log.trace("found existing globals init function: %s", decl);
		    globalInitFn = decl;
		    break;
		}
	    }

	    Block block = new Block();
	    FunctionLVarDecl scratchIntVar = new FunctionLVarDecl("arraySize", TypeDecl.TYPE_INTEGER);
	    int scratchIntVarSlot = 0, scratchClosureFirstLocal = 1;
	    if (globalInitFn != null) {
		globalInitFn.lvars.add(0, scratchIntVar);
		scratchIntVarSlot = globalInitFn.paramTypes.size();
		scratchClosureFirstLocal = globalInitFn.paramTypes.size() + globalInitFn.lvars.size();
	    }

	    log.trace("mutating existing globals init function: spare local in slot %s, spare closure-local at slot %s", scratchIntVarSlot,
		    scratchClosureFirstLocal);

	    Iterator<GlobalsBlock> itx1 = codeChunk.globalsDecls.iterator();
	    while (itx1.hasNext()) {
		Iterator<GlobalsVarDecl> itx2 = itx1.next().iterator();
		while (itx2.hasNext()) {
		    GlobalsVarDecl decl = itx2.next();
		    if (!decl.isArrayType && decl.rhs != null) {
			log.trace("mutator introducing set statements for global `%s`", decl.nameOfGlobalVar);
			SetVariableStatement allocSetVarNode = new SetVariableStatement();
			allocSetVarNode.nameOfLocalVar = decl.nameOfGlobalVar;
			allocSetVarNode.rhs = decl.rhs;
			block.add(allocSetVarNode);
		    } else if (decl.isArrayType) {
			log.trace("mutator introducing newarray statements for global `%s`", decl.nameOfGlobalVar);
			DirectAssembly allocDirectAsmNode = new DirectAssembly();
			List<Opcode> oplist;
			allocDirectAsmNode.opcodes = oplist = new LinkedList<>();

			SetVariableStatement allocSetArrayVarNode = new SetVariableStatement();
			allocSetArrayVarNode.nameOfLocalVar = scratchIntVar.nameOfLocalVar;
			allocSetArrayVarNode.rhs = decl.rhs;
			block.add(allocSetArrayVarNode);

			oplist.add(Opcodes.INCLOSURE);

			List<Object> getlocalParams = new LinkedList<>();
			getlocalParams.add(scratchIntVarSlot);
			oplist.add(new OpcodeControlBitmask(Opcodes.GETLOCAL, getlocalParams));

			List<Object> newupvarParams = new LinkedList<>();
			newupvarParams.add(typeMachine.getTypeNumber(decl.valueType));
			newupvarParams.add(scratchClosureFirstLocal);
			oplist.add(new OpcodeControlBitmask(Opcodes.NEWUPVAR, Opcodes.BITMASK_NEWUPVAR_ARRAY, newupvarParams));

			getlocalParams = new LinkedList<>();
			getlocalParams.add(scratchClosureFirstLocal);
			oplist.add(new OpcodeControlBitmask(Opcodes.GETLOCAL, getlocalParams));

			List<Object> setglobalParams = new LinkedList<>();
			setglobalParams.add(decl.nameOfGlobalVar);
			oplist.add(new OpcodeControlBitmask(Opcodes.SETGLOBAL, setglobalParams));

			oplist.add(Opcodes.ENDCLOSURE);

			block.add(allocDirectAsmNode);
		    }
		}
	    }

	    if (globalInitFn == null) {
		log.trace("building new global init function");
		globalInitFn = new FunctionDecl(FunctionDeclType.USER);
		globalInitFn.returnType = TypeDecl.TYPE_NOTHING;
		globalInitFn.lvars = new LinkedList<>();
		globalInitFn.lvars.add(scratchIntVar);
		globalInitFn.paramNames = new LinkedList<>();
		globalInitFn.paramTypes = new LinkedList<>();
		globalInitFn.name = "<ginit>";
		globalInitFn.block = block;
		codeChunk.ufuncDecls.add(globalInitFn);
	    } else {
		log.trace("applying mutation to global init function");
		block.addAll(globalInitFn.block);
		globalInitFn.block = block;
	    }
	}
    }

    /**
     * Internal: Assembles EXPORTS symbols.
     *
     * @param dstLinkObj The destination linker object
     * @param codeChunk  The AST root node
     * @param log        The logger or <code>null</code>
     *
     * @throws Exception
     */
    void assembleChunkLinkerExportData(LinkerObjectFile dstLinkObj, Chunk codeChunk, Log log) throws Exception {
	log.debug("assembling chunk export data");
	if (codeChunk.libExportsDecls != null && !codeChunk.libExportsDecls.isEmpty()) {
	    Iterator<LibraryBlock> itx = codeChunk.libExportsDecls.iterator();
	    while (itx.hasNext()) {
		LibraryBlock exportDecl = itx.next();
		Iterator<LibraryReference> itx1 = exportDecl.iterator();
		while (itx1.hasNext()) {
		    LibraryReference referentSymbolSet = itx1.next();
		    switch (referentSymbolSet.type) {
			case TYPE:
			    typeMachine.assembleTypeExport(dstLinkObj, codeChunk, exportDecl, referentSymbolSet, log);
			    break;
			case GLOBAL:
			    globalsMachine.assembleGlobalExport(dstLinkObj, typeMachine, codeChunk, exportDecl, referentSymbolSet, log);
			    break;
			case FUNCTION:
			    functionMachine.assembleFunctionExport(dstLinkObj, typeMachine, codeChunk, exportDecl, referentSymbolSet, log);
			    break;
			default:
			    throw new Exception("unsupported referent symbol type");
		    }
		}
	    }
	}
    }

    void assembleChunkBytecodeData(LinkerObjectFile dstLinkObj, LogicBytecodeFile dstFnAssembleObj, Chunk codeChunk, Log log) throws Exception {
	log.debug("assembling chunk bytecode data");
	if (codeChunk.nfuncDecls != null && !codeChunk.nfuncDecls.isEmpty()) {
	    log.debug("assembling chunk native function declarations");
	    Iterator<FunctionDecl> itx = codeChunk.nfuncDecls.iterator();
	    while (itx.hasNext())
		functionMachine.assembleNativeFunction(dstLinkObj, dstFnAssembleObj, typeMachine, codeChunk, itx.next(), log);
	}

	if (codeChunk.ufuncDecls != null && !codeChunk.ufuncDecls.isEmpty()) {
	    log.debug("assembling chunk user function declarations");
	    Iterator<FunctionDecl> itx = codeChunk.ufuncDecls.iterator();
	    while (itx.hasNext())
		functionMachine.assembleUserFunction(dstLinkObj, dstFnAssembleObj, typeMachine, codeChunk, itx.next(), log);
	}
    }

}
