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

/**
 * @author Lochie
 */
public class TypeAssembler {

    BiDiMap<Integer, TypeDecl> assembledTypeTable = new BiDiMap<>();
    BiDiMap<String, TypeDecl> assembledTypeTableNameIndex = new BiDiMap<>();

    public TypeAssembler() {
	reset();
    }

    public void reset() {
	this.assembledTypeTable.clear();
	this.assembledTypeTableNameIndex.clear();
    }

    public void assembleTypeDecl(LinkerObjectFile dstLinkObj, Chunk c, TypeDecl typeDecl) throws Exception {
	if (assembledTypeTableNameIndex.containsKey(typeDecl.typeName))
	    throw new Exception(String.format("TypeAssembler#assembleTypeDecl: duplicate type-name `%s`", typeDecl.typeName));
	if (typeDecl.type == TypeDeclType.EXTENDS_HANDLE) {
	    TypesEntry asCompiled = new TypesEntry();
	    asCompiled.name = typeDecl.typeName;
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_TYPES_HANDLE;
	    internalAssembleType(typeDecl, asCompiled);
	    dstLinkObj.typesTable.add(asCompiled);
	} else if (typeDecl.type == TypeDeclType.EXTENDS_IMPORTS || typeDecl.type == TypeDeclType.EXTENDS_TYPENAME) {
	    TypesEntry asCompiled = new TypesEntry();
	    asCompiled.name = typeDecl.typeName;
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_TYPES_USER;
	    asCompiled.superName = typeDecl.ancestorTypeName;
	    internalAssembleType(typeDecl, asCompiled);
	    dstLinkObj.typesTable.add(asCompiled);
	} else
	    throw new Exception(String.format("TypeAssembler#assembleTypeDecl: unsupported type type %s", typeDecl.type.name()));
    }

    public void assembleTypeImport(LinkerObjectFile dstLinkObj, Chunk c, LibraryBlock collection, LibraryReference referent) throws Exception {
	if (referent.isWildcard)
	    // TODO: this is technically possible, but for now take the easier route:
	    throw new IllegalStateException("Cannot import types by wildcard operator");
	else {
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
		    throw new Exception(String.format("TypeAssembler#assembleTypeImport: missing typetoken placeholder for type %s", name));
		if (assembledTypeTableNameIndex.containsKey(name))
		    throw new Exception(String.format("TypeAssembler#assembleTypeImport: duplicate type-name `%s`", name));
		int index = internalNextTypeSlot();
		labelList.add(name);
		slotList.add(index);
		assembledTypeTable.put(index, placeholderType);
	    }
	    dstLinkObj.importsTable.add(asCompiled);
	}
    }

    public void assembleTypeExport(LinkerObjectFile dstLinkObj, Chunk c, LibraryBlock collection, LibraryReference referent) throws Exception {

	if (referent.isWildcard) {
	    ExportsEntry asCompiled = new ExportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_EXPORTS_TYPE;
	    asCompiled.name = collection.symbol;
	    dstLinkObj.exportsTable.add(asCompiled);
	} else {
	    ExportsEntry asCompiled = new ExportsEntry();
	    asCompiled.typeflags = LinkerObjectFile.TYPEFLAGS_EXPORTS_TYPE;
	    asCompiled.name = collection.symbol;

	    List<String> labelList = (asCompiled.labels = new LinkedList<>());
	    Iterator<String> itx = referent.tokens.iterator();
	    String name = null;
	    while (itx.hasNext()) {
		TypeDecl placeholderType = c.findTypeDecl(name = itx.next());
		if (placeholderType == null)
		    throw new Exception(String.format("TypeAssembler#assembleTypeExport: missing typetoken placeholder for type %s", name));
		if (getTypeNumber(placeholderType) == 0)
		    throw new Exception(String.format("TypeAssembler#assembleTypeExport: the type `%s` is not defined", name));
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

    public TypeDecl checkType(TypeDecl typeObj) throws Exception {
	return checkType(typeObj.typeName);
    }

    public TypeDecl checkType(String nameOfType) throws Exception {
	return assembledTypeTableNameIndex.get(nameOfType);
    }

    public int getTypeNumber(TypeDecl compiledType) throws Exception {
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
	    throw new Exception(String.format("The type %s has not been compiled", compiledType.typeName));
	return (int) boxOfVal;
    }

}
