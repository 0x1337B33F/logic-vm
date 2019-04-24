package logicscript.assembler.writer;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logictype.BiDiMap;

/**
 *
 * @author Lochie
 */
public class GlobalsAssembler {

    BiDiMap<GlobalsVarDecl, GlobalsEntry> assembledGlobalsTable = new BiDiMap<>();

    public GlobalsAssembler() {
	reset();
    }

    public void reset() {
	this.assembledGlobalsTable.clear();
    }

    public void assembleGlobalDeclaration(LinkerObjectFile dstLinkObj, TypeAssembler typeCompilerMachine, Chunk declaringChunk,
	    GlobalsBlock declaringBlock, GlobalsVarDecl decl) throws Exception {

	GlobalsEntry asCompiled = new GlobalsEntry();
	asCompiled.name = decl.nameOfGlobalVar;
	asCompiled.type = typeCompilerMachine.getTypeNumber(decl.valueType);
	asCompiled.globalsflags = 0; // TODO: native globals
	if (decl.isArrayType)
	    asCompiled.globalsflags |= LinkerObjectFile.TYPEFLAGS_GLOBAL_ISARRAY;
	assembledGlobalsTable.put(decl, asCompiled);
	dstLinkObj.globalsTable.add(asCompiled);
    }

    public void assembleGlobalImport(LinkerObjectFile dstLinkObj, TypeAssembler typeCompilerMachine, Chunk declaringChunk,
	    LibraryBlock importBlock,
	    LibraryReference referentGlobals) throws Exception {

	if (referentGlobals.isWildcard) {
	    ImportsEntry asCompiled = new ImportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL | LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD;
	    asCompiled.name = importBlock.symbol;
	    dstLinkObj.importsTable.add(asCompiled);
	} else {
	    ImportsEntry asCompiled = new ImportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL;
	    asCompiled.name = importBlock.symbol;
	    asCompiled.labels = referentGlobals.tokens;
	    dstLinkObj.importsTable.add(asCompiled);
	}
    }

    public void assembleGlobalExport(LinkerObjectFile dstLinkObj, TypeAssembler typeCompilerMachine, Chunk declaringChunk,
	    LibraryBlock exportBlock, LibraryReference referentGlobals) throws Exception {
	ExportsEntry asCompiled = new ExportsEntry();
	asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_EXPORTS_GLOBAL;
	asCompiled.name = exportBlock.symbol;
	asCompiled.labels = referentGlobals.tokens;
	dstLinkObj.exportsTable.add(asCompiled);
    }

}
