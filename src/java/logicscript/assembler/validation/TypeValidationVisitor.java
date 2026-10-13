package logicscript.assembler.validation;

import java.util.Iterator;
import java.util.List;

import logicscript.ast.visitors.VoidAssemblerASTVisitor;
import logicscript.ast.Chunk;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.FunctionLVarDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.decl.TypeDecl.TypeDeclType;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logicscript.ast.nodes.importexport.LibraryReference.LibraryReferenceType;
import logicscript.ast.nodes.stmt.Block;

// FIXME: there are two problems here; the first is that the 
// types are dyamically linked and not statically linked; the
// second is that a reset() is needed for each compile pass of
// each script, since each script has its own set of IMPORT/EXPORT
// stanzas
public class TypeValidationVisitor extends VoidAssemblerASTVisitor {

    volatile Chunk generatedDecl;

    List<TypeDecl> importedTypeTable;
    List<TypeDecl> exportedTypeTable;

    List<TypeDecl> userTypesTable;

    public TypeValidationVisitor(List<TypeDecl> i, List<TypeDecl> e, List<TypeDecl> u) {
	importedTypeTable = i;
	exportedTypeTable = e;
	userTypesTable = u;
    }

    @Override
    public void visitImportDecl(LibraryBlock b) throws Exception {
	Iterator<LibraryReference> itx = b.iterator();
	while (itx.hasNext()) {
	    LibraryReference ref = itx.next();
	    if (ref.type == LibraryReferenceType.TYPE) {
		Iterator<String> tokens = ref.tokens.iterator();
		while (tokens.hasNext()) {
		    String typeName = tokens.next();
		    Iterator<TypeDecl> itx1 = generatedDecl.etypeDecls.iterator();
		    TypeDecl matchedDecl = null;
		    while (itx1.hasNext()) {
			TypeDecl implDeclRef = itx1.next();
			if (implDeclRef.typeName.equals(typeName)) {
			    matchedDecl = implDeclRef;
			    break;
			}
		    }

		    if (matchedDecl == null)
			throw new Exception(String.format(
				"Missing referent type for type-token `%s` defined in IMPORTS block", typeName));
		}
	    }
	}
    }

    @Override
    public void visitExportDecl(LibraryBlock b) throws Exception {
	Iterator<LibraryReference> itx = b.iterator();
	while (itx.hasNext()) {
	    LibraryReference ref = itx.next();
	    if (ref.type == LibraryReferenceType.TYPE) {
		Iterator<String> tokens = ref.tokens.iterator();
		while (tokens.hasNext()) {
		    String typeName = tokens.next();
		    Iterator<TypeDecl> itx1 = generatedDecl.utypeDecls.iterator();
		    TypeDecl matchedDecl = null;
		    while (itx1.hasNext()) {
			TypeDecl implDeclRef = itx1.next();
			if (implDeclRef.typeName.equals(typeName)) {
			    matchedDecl = implDeclRef;
			    break;
			}
		    }
		    if (matchedDecl == null)
			throw new Exception(String.format(
				"Missing referent type for type-token `%s` defined in EXPORTS block", typeName));
		}
	    }
	}
    }

    @Override
    public void visitGlobalsDecl(GlobalsBlock b) throws Exception {
	Iterator<GlobalsVarDecl> itx = b.iterator();
	while (itx.hasNext()) {
	    GlobalsVarDecl decl = itx.next();
	    if (decl.valueType == null)
		throw new Exception(String.format("Missing type for global variable `%s` (defined at %s)",
			decl.nameOfGlobalVar, b.location));
	}
    }

    @Override
    public void visitTypeDecl(TypeDecl t) throws Exception {
	if (t.type == TypeDeclType.EXTENDS_HANDLE)
	    return;
	if (t.type == TypeDeclType.SYSTEM_TYPE)
	    throw new Exception("May not define SYSTEM_TYPE");

	Iterator<TypeDecl> itx = generatedDecl.utypeDecls.iterator();
	while (itx.hasNext()) {
	    TypeDecl typeDecl = itx.next();
	    if (typeDecl.typeName.equals(t.ancestorTypeName))
		return;
	}

	itx = generatedDecl.etypeDecls.iterator();
	while (itx.hasNext()) {
	    TypeDecl typeDecl = itx.next();
	    if (typeDecl.typeName.equals(t.ancestorTypeName))
		return;
	}

	throw new Exception(String.format("Missing type for type declaration `%s` (defined at %s)", t.ancestorTypeName, t.location));
    }

    @Override
    public void onVisitChunk(Chunk c) throws Exception {
	generatedDecl = c;
    }

    @Override
    public void onVisitChunkEnd(Chunk c) throws Exception {
	generatedDecl = null;
    }

    @Override
    public void onVisitUFuncDecl(FunctionDecl ufunc) throws Exception {
	// TODO Auto-generated method stub

    }

    @Override
    public void onVisitUFuncDeclEnd(FunctionDecl ufunc) throws Exception {
	// TODO Auto-generated method stub

    }

    @Override
    public void onVisitFuncDeclLVar(FunctionLVarDecl vd) throws Exception {
	// TODO Auto-generated method stub

    }

    @Override
    public void onVisitFuncDeclLVarEnd(FunctionLVarDecl vd) throws Exception {
	// TODO Auto-generated method stub

    }

    /**
     * Visit entire function body; don't do descent:
     */
    @Override
    public void visitFuncDeclBlock(Block b) throws Exception {

    }
}
