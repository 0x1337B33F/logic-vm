package logicscript.assembler.writer;

import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.GlobalsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicscript.assembler.writer.TypeAssembler.TypeAssemblerLookupException;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logictype.BiDiMap;
import logictype.Log;

/**
 *
 * @author Lochie
 */
public class GlobalsAssembler {

    static class GlobalsAssemblerGlobalException extends Exception {

	final String globalName;

	public GlobalsAssemblerGlobalException(String globalName, String message) {
	    super(message);
	    this.globalName = globalName;
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder(getClass().getName());
	    sb.append(": ").append(getMessage()).append("\r\n");
	    if (this.globalName != null)
		sb.append("\tglobal-name: `").append(this.globalName).append("`\r\n");
	    return sb.toString();
	}
    }

    public static class GlobalsAssemblerLookupException extends GlobalsAssemblerGlobalException {

	public GlobalsAssemblerLookupException(String globalName, String message) {
	    super(globalName, message);
	}
    }

    public static class GlobalsAssemblerGeneratorException extends GlobalsAssemblerGlobalException {

	public GlobalsAssemblerGeneratorException(String globalName, String message) {
	    super(globalName, message);
	}

    }

    BiDiMap<GlobalsVarDecl, GlobalsEntry> assembledGlobalsTable = new BiDiMap<>();

    public GlobalsAssembler() {
	reset();
    }

    public void reset() {
	this.assembledGlobalsTable.clear();
    }

    public void assembleGlobalDeclaration(LinkerObjectFile dstLinkObj, TypeAssembler typeCompilerMachine, Chunk declaringChunk,
	    GlobalsBlock declaringBlock, GlobalsVarDecl decl, Log log) throws TypeAssemblerLookupException, GlobalsAssemblerGeneratorException {
	log.trace("assembling global declaration: global `%s`, %s", decl.nameOfGlobalVar, (decl.isArrayType) ? "array" : "literal");
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
	    LibraryBlock importBlock, LibraryReference referentGlobals, Log log) throws GlobalsAssemblerGeneratorException {
	if (referentGlobals.isWildcard) {
	    log.trace("assembling global import declaration: symbol `%s`, wildcard import", importBlock.symbol);
	    ImportsEntry asCompiled = new ImportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL | LinkerObjectFile.TYPEFLAGS_IMPORTS_WILDCARD;
	    asCompiled.name = importBlock.symbol;
	    dstLinkObj.importsTable.add(asCompiled);
	} else {
	    log.trace("assembling global import declaration: symbol `%s`", importBlock.symbol);
	    ImportsEntry asCompiled = new ImportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_IMPORTS_GLOBAL;
	    asCompiled.name = importBlock.symbol;
	    asCompiled.labels = referentGlobals.tokens;
	    dstLinkObj.importsTable.add(asCompiled);
	}
    }

    public void assembleGlobalExport(LinkerObjectFile dstLinkObj, TypeAssembler typeCompilerMachine, Chunk declaringChunk,
	    LibraryBlock exportBlock, LibraryReference referentGlobals, Log log) throws GlobalsAssemblerGeneratorException {
	log.trace("assembling global export declaration: symbol `%s`", exportBlock.symbol);
	ExportsEntry asCompiled = new ExportsEntry();
	asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_EXPORTS_GLOBAL;
	asCompiled.name = exportBlock.symbol;
	asCompiled.labels = referentGlobals.tokens;
	dstLinkObj.exportsTable.add(asCompiled);
    }

}
