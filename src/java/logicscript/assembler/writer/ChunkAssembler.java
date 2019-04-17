package logicscript.assembler.writer;

import java.util.Iterator;
import java.util.LinkedList;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LogicBytecodeFile;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionDecl.FunctionDeclType;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logicscript.ast.nodes.stmt.Block;
import logicscript.ast.nodes.stmt.ef.SetVariableStatement;

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

    /**
     * Reset the chunk assembler so that a new chunk can be assembled without reallocation of the assembler.
     */
    public void reset() {
	typeMachine.reset();
	functionMachine.reset();
	globalsMachine.reset();
    }

    /**
     * <p>
     * Assemble a Chunk and place the resultant struct(s) in the provided destination linker and destination bytecode object(s), which must
     * be allocated externally. For an AST Chunk, the following occurs:</p>
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
     *
     * @throws Exception Any checked exception which occurs during assembly of the AST
     */
    public void assembleChunk(
	    Chunk codeChunk,
	    LinkerObjectFile dstLinkObj,
	    LogicBytecodeFile dstFnAssembleObj) throws Exception {
	/*  
	    assemble linker information first; this ensures the state machines 
	    are in the correct state for subsequent bytecode compilation:
	 */
	assembleChunkLinkerImportData(dstLinkObj, codeChunk);
	assembleChunkGlobalBytecodeData(dstLinkObj, dstFnAssembleObj, codeChunk);
	assembleChunkBytecodeData(dstLinkObj, dstFnAssembleObj, codeChunk);
	assembleChunkLinkerExportData(dstLinkObj, codeChunk);
    }

    /**
     * Internal: Assembles IMPORTS symbols.
     *
     * @param dstLinkObj The destination linker object
     * @param codeChunk  The AST root node
     *
     * @throws Exception
     */
    void assembleChunkLinkerImportData(LinkerObjectFile dstLinkObj, Chunk codeChunk) throws Exception {
	if (codeChunk.libImportsDecls != null && !codeChunk.libImportsDecls.isEmpty()) {
	    Iterator<LibraryBlock> itx = codeChunk.libImportsDecls.iterator();
	    while (itx.hasNext()) {
		LibraryBlock importDecl = itx.next();
		Iterator<LibraryReference> itx1 = importDecl.iterator();
		while (itx1.hasNext()) {
		    LibraryReference referentSymbolSet = itx1.next();
		    switch (referentSymbolSet.type) {
			case TYPE:
			    typeMachine.assembleTypeImport(dstLinkObj, codeChunk, importDecl, referentSymbolSet);
			    break;
			case GLOBAL:
			    globalsMachine.assembleGlobalImport(dstLinkObj, typeMachine, codeChunk, importDecl, referentSymbolSet);
			    break;
			case FUNCTION:
			    functionMachine.assembleFunctionImport(dstLinkObj, typeMachine, codeChunk, importDecl, referentSymbolSet);
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
		typeMachine.assembleTypeDecl(dstLinkObj, codeChunk, itx.next());
	}

	if (codeChunk.globalsDecls != null && !codeChunk.globalsDecls.isEmpty()) {
	    Iterator<GlobalsBlock> itx = codeChunk.globalsDecls.iterator();
	    while (itx.hasNext()) {
		GlobalsBlock block = itx.next();
		Iterator<GlobalsVarDecl> itx1 = block.iterator();
		while (itx1.hasNext())
		    globalsMachine.assembleGlobalDeclaration(dstLinkObj, typeMachine, codeChunk, block, itx1.next());
	    }
	}
    }

    /**
     * Internal: Assembles GLOBALS declarations.
     *
     * @param dstLinkObj       The destination linker object
     * @param dstFnAssembleObj The destination bytecode object
     * @param codeChunk        The root AST node
     *
     * @throws Exception
     */
    void assembleChunkGlobalBytecodeData(LinkerObjectFile dstLinkObj, LogicBytecodeFile dstFnAssembleObj, Chunk codeChunk) throws Exception {
	if (codeChunk.globalsDecls != null && !codeChunk.globalsDecls.isEmpty()) {
	    FunctionDecl globalInitFn = null;
	    Iterator<FunctionDecl> itx = codeChunk.ufuncDecls.iterator();
	    while (itx.hasNext()) {
		FunctionDecl decl = itx.next();
		if (decl.name.equals("<ginit>")) {
		    globalInitFn = decl;
		    break;
		}
	    }

	    Block block = new Block();

	    Iterator<GlobalsBlock> itx1 = codeChunk.globalsDecls.iterator();
	    while (itx1.hasNext()) {
		Iterator<GlobalsVarDecl> itx2 = itx1.next().iterator();
		while (itx2.hasNext()) {
		    GlobalsVarDecl decl = itx2.next();
		    if (decl.rhs != null) {
			SetVariableStatement allocSetVarNode = new SetVariableStatement();
			allocSetVarNode.nameOfLocalVar = decl.nameOfGlobalVar;
			allocSetVarNode.rhs = decl.rhs;
			block.add(allocSetVarNode);
		    }
		}
	    }

	    if (globalInitFn == null) {
		globalInitFn = new FunctionDecl(FunctionDeclType.USER);
		globalInitFn.returnType = TypeDecl.TYPE_NOTHING;
		globalInitFn.lvars = new LinkedList<>();
		globalInitFn.paramNames = new LinkedList<>();
		globalInitFn.paramTypes = new LinkedList<>();
		globalInitFn.name = "<ginit>";
		globalInitFn.block = block;
		codeChunk.ufuncDecls.add(globalInitFn);
	    } else {
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
     *
     * @throws Exception
     */
    void assembleChunkLinkerExportData(LinkerObjectFile dstLinkObj, Chunk codeChunk) throws Exception {
	if (codeChunk.libExportsDecls != null && !codeChunk.libExportsDecls.isEmpty()) {
	    Iterator<LibraryBlock> itx = codeChunk.libExportsDecls.iterator();
	    while (itx.hasNext()) {
		LibraryBlock exportDecl = itx.next();
		Iterator<LibraryReference> itx1 = exportDecl.iterator();
		while (itx1.hasNext()) {
		    LibraryReference referentSymbolSet = itx1.next();
		    switch (referentSymbolSet.type) {
			case TYPE:
			    typeMachine.assembleTypeExport(dstLinkObj, codeChunk, exportDecl, referentSymbolSet);
			    break;
			case GLOBAL:
			    globalsMachine.assembleGlobalExport(dstLinkObj, typeMachine, codeChunk, exportDecl, referentSymbolSet);
			    break;
			case FUNCTION:
			    functionMachine.assembleFunctionExport(dstLinkObj, typeMachine, codeChunk, exportDecl, referentSymbolSet);
			    break;
			default:
			    throw new Exception("unsupported referent symbol type");
		    }
		}
	    }
	}
    }

    void assembleChunkBytecodeData(LinkerObjectFile dstLinkObj, LogicBytecodeFile dstFnAssembleObj, Chunk codeChunk) throws Exception {
	if (codeChunk.nfuncDecls != null && !codeChunk.nfuncDecls.isEmpty()) {
	    Iterator<FunctionDecl> itx = codeChunk.nfuncDecls.iterator();
	    while (itx.hasNext())
		functionMachine.assembleNativeFunction(dstLinkObj, dstFnAssembleObj, typeMachine, codeChunk, itx.next());
	}

	if (codeChunk.ufuncDecls != null && !codeChunk.ufuncDecls.isEmpty()) {
	    Iterator<FunctionDecl> itx = codeChunk.ufuncDecls.iterator();
	    while (itx.hasNext())
		functionMachine.assembleUserFunction(dstLinkObj, dstFnAssembleObj, typeMachine, codeChunk, itx.next());
	}
    }

}
