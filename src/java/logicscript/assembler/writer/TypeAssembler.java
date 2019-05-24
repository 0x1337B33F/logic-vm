package logicscript.assembler.writer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.bytecode.LinkerObjectFile.ExportsEntry;
import logicbyte.bytecode.LinkerObjectFile.ImportsEntry;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.decl.TypeDecl.TypeDeclType;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logictype.BiDiMap;
import logictype.Log;

/**
 * @author Lochie
 */
public class TypeAssembler {

    static class TypeAssemblerTypeException extends Exception {

	final String typeName;

	public TypeAssemblerTypeException(String typeName, String message) {
	    super(message);
	    this.typeName = typeName;
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder(getClass().getName());
	    sb.append(": ").append(getMessage()).append("\r\n");
	    if (this.typeName != null)
		sb.append("\ttype-name: `").append(this.typeName).append("`\r\n");
	    return sb.toString();
	}
    }

    public static class TypeAssemblerLookupException extends TypeAssemblerTypeException {

	public TypeAssemblerLookupException(String typeName, String message) {
	    super(typeName, message);
	}

    }

    public static class TypeAssemblerGeneratorException extends TypeAssemblerTypeException {

	public TypeAssemblerGeneratorException(String typeName, String message) {
	    super(typeName, message);
	}
    }

    BiDiMap<Integer, TypeDecl> assembledTypeTable = new BiDiMap<>();
    BiDiMap<String, TypeDecl> assembledTypeTableNameIndex = new BiDiMap<>();

    public TypeAssembler() {
	reset();
    }

    public void reset() {
	this.assembledTypeTable.clear();
	this.assembledTypeTableNameIndex.clear();
    }

    public void assembleTypeDecl(LinkerObjectFile dstLinkObj, Chunk c, TypeDecl typeDecl, Log log) throws TypeAssemblerGeneratorException {
	if (assembledTypeTableNameIndex.containsKey(typeDecl.typeName))
	    throw new TypeAssemblerGeneratorException(typeDecl.typeName, "duplicate type-name");
	if (typeDecl.type == TypeDeclType.EXTENDS_HANDLE) {
	    log.trace("compiling type-declaration: type `%s`, handle type", typeDecl.typeName);
	    TypesEntry asCompiled = new TypesEntry();
	    asCompiled.name = typeDecl.typeName;
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_TYPES_HANDLE;
	    internalAssembleType(typeDecl, asCompiled);
	    dstLinkObj.typesTable.add(asCompiled);
	} else if (typeDecl.type == TypeDeclType.EXTENDS_IMPORTS || typeDecl.type == TypeDeclType.EXTENDS_TYPENAME) {
	    log.trace("compiling type-declaration: type `%s`, supertype `%s`", typeDecl.typeName, typeDecl.ancestorTypeName);
	    TypesEntry asCompiled = new TypesEntry();
	    asCompiled.name = typeDecl.typeName;
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_TYPES_USER;
	    asCompiled.superName = typeDecl.ancestorTypeName;
	    internalAssembleType(typeDecl, asCompiled);
	    dstLinkObj.typesTable.add(asCompiled);
	} else
	    throw new TypeAssemblerGeneratorException(typeDecl.typeName, String.format("unsupported type type %s", typeDecl.type.name()));
    }

    public void assembleTypeImport(LinkerObjectFile dstLinkObj, Chunk c, LibraryBlock collection, LibraryReference referent, Log log) throws TypeAssemblerGeneratorException {
	if (referent.isWildcard)
	    // TODO: this is technically possible, but for now take the easier route:
	    throw new TypeAssemblerGeneratorException(null, "Cannot import types by wildcard operator");
	else {
	    log.trace("compiling type import entry: symbol `%s`", collection.symbol);
	    ImportsEntry asCompiled = new ImportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_IMPORTS_TYPE;
	    asCompiled.name = collection.symbol;

	    List<String> labelList = (asCompiled.labels = new LinkedList<>());
	    List<Integer> slotList = (asCompiled.slots = new LinkedList<>());

	    Iterator<String> itx = referent.tokens.iterator();
	    String name = null;
	    while (itx.hasNext()) {
		TypeDecl placeholderType = c.findTypeDecl(name = itx.next());
		if (placeholderType == null)
		    throw new TypeAssemblerGeneratorException(name, "missing typetoken placeholder for type");
		if (assembledTypeTableNameIndex.containsKey(name))
		    throw new TypeAssemblerGeneratorException(name, "duplicate type-name");
		int index = internalNextTypeSlot();
		log.trace("compiling type import entry: symbol `%s`, slot %s, type `%s`", collection.symbol, index, name);
		labelList.add(name);
		slotList.add(index);
		assembledTypeTable.put(index, placeholderType);
	    }
	    dstLinkObj.importsTable.add(asCompiled);
	}
    }

    public void assembleTypeExport(LinkerObjectFile dstLinkObj, Chunk c, LibraryBlock collection, LibraryReference referent, Log log) throws TypeAssemblerLookupException, TypeAssemblerGeneratorException {
	if (referent.isWildcard) {
	    ExportsEntry asCompiled = new ExportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_EXPORTS_TYPE;
	    asCompiled.name = collection.symbol;
	    dstLinkObj.exportsTable.add(asCompiled);
	} else {
	    log.trace("compiling type export entry: symbol `%s`", collection.symbol);
	    ExportsEntry asCompiled = new ExportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_EXPORTS_TYPE;
	    asCompiled.name = collection.symbol;

	    List<String> labelList = (asCompiled.labels = new LinkedList<>());
	    Iterator<String> itx = referent.tokens.iterator();
	    String name = null;
	    while (itx.hasNext()) {
		TypeDecl placeholderType = c.findTypeDecl(name = itx.next());
		if (placeholderType == null)
		    throw new TypeAssemblerGeneratorException(name, "missing typetoken placeholder");
		if (getTypeNumber(placeholderType) == 0)
		    throw new TypeAssemblerLookupException(name, "type not assembled");
		log.trace("compiling type export entry: symbol `%s`, type `%s`", collection.symbol, name);
		labelList.add(name);
	    }
	    dstLinkObj.exportsTable.add(asCompiled);
	}
    }

    int internalNextTypeSlot() {
	return 7 + assembledTypeTable.size();
    }

    void internalAssembleType(TypeDecl declInst, TypesEntry impl) {
	int index = internalNextTypeSlot();
	impl.slot = index;
	assembledTypeTable.put(index, declInst);
	assembledTypeTableNameIndex.put(declInst.typeName, declInst);
    }

    public TypeDecl checkType(TypeDecl typeObj) throws TypeAssemblerLookupException {
	return checkType(typeObj.typeName);
    }

    public TypeDecl checkType(String nameOfType) throws TypeAssemblerLookupException {
	return assembledTypeTableNameIndex.get(nameOfType);
    }

    public int getTypeNumber(TypeDecl compiledType) throws TypeAssemblerLookupException {
	if ( compiledType == TypeDecl.TYPE_ANY )
	    return -1;
	if (compiledType == TypeDecl.TYPE_NOTHING)
	    return 0;
	if (compiledType == TypeDecl.TYPE_HANDLE)
	    return 1;
	if (compiledType == TypeDecl.TYPE_CODE)
	    return 2;
	if (compiledType == TypeDecl.TYPE_BOOLEAN)
	    return 3;
	if (compiledType == TypeDecl.TYPE_INTEGER)
	    return 4;
	if (compiledType == TypeDecl.TYPE_REAL)
	    return 5;
	if (compiledType == TypeDecl.TYPE_STRING)
	    return 6;

	Integer boxOfVal = assembledTypeTable.reverse().get(compiledType);
	if (boxOfVal == null)
	    throw new TypeAssemblerLookupException(compiledType.typeName, "type not assembled");
	return (int) boxOfVal;
    }

}
