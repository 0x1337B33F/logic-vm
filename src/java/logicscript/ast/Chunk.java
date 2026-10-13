package logicscript.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import logicscript.ast.nodes.decl.FunctionDecl;
import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.decl.TypeDecl.TypeDeclType;
import logicscript.ast.nodes.globals.GlobalsBlock;
import logicscript.ast.nodes.globals.GlobalsVarDecl;
import logicscript.ast.nodes.importexport.LibraryBlock;
import logicscript.ast.nodes.importexport.LibraryReference;
import logicscript.ast.nodes.importexport.LibraryReference.LibraryReferenceType;

/**
 * The AST object generated when a file is parsed. Depending on the file contents, the imports, exports, globals, types, nfuncs &amp; ufuncs
 * may contain a List, be a value or explicitly null to indicate no such section(s) exist within the parsed script.
 *
 *
 * The AST object(s) offer no thread-safe synchronization.
 *
 * @author Lochie
 */
public class Chunk implements Visitable {

    /**
     * List of IMPORTS blocks
     */
    public List<LibraryBlock> libImportsDecls;

    /**
     * List of EXPORTS blocks
     */
    public List<LibraryBlock> libExportsDecls;

    /**
     * List of GLOBALS blocks
     */
    public List<GlobalsBlock> globalsDecls;

    /**
     * List of all defined user types
     */
    public List<TypeDecl> utypeDecls;

    /**
     * List of all synthetically generated types, such as those used to represent TYPE symbols used by IMPORTSBLOCK(s)
     */
    public List<TypeDecl> etypeDecls;

    /**
     * List of all synthetically generated types whose full symbol cannot be resolved (ie, it might be represented by a wildcard import)
     */
    public List<TypeDecl> mtypeDecls;

    /**
     * List of all native function declarations
     */
    public List<FunctionDecl> nfuncDecls;

    /**
     * List of all user function declarations
     */
    public List<FunctionDecl> ufuncDecls;

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (libImportsDecls != null)
	    synchronized ( libImportsDecls ) {
		Iterator<LibraryBlock> itx = libImportsDecls.iterator();
		while (itx.hasNext())
		    v.visitImportDecl(itx.next());
	    }

	if (libExportsDecls != null)
	    synchronized ( libExportsDecls ) {
		Iterator<LibraryBlock> itx = libExportsDecls.iterator();
		while (itx.hasNext())
		    v.visitExportDecl(itx.next());
	    }

	if (globalsDecls != null)
	    synchronized ( globalsDecls ) {
		Iterator<GlobalsBlock> itx = globalsDecls.iterator();
		while (itx.hasNext()) {
		    GlobalsBlock block = itx.next();
		    v.visitGlobalsDecl(block);
		    Iterator<GlobalsVarDecl> itx1 = block.iterator();
		    while (itx1.hasNext()) {
			GlobalsVarDecl decl = itx1.next();
			v.visitGlobalsVarDecl(decl);
			if (decl.rhs != null)
			    v.visitExpression(decl.rhs);
		    }
		}
	    }

	if (utypeDecls != null)
	    synchronized ( utypeDecls ) {
		Iterator<TypeDecl> itx = utypeDecls.iterator();
		while (itx.hasNext())
		    v.visitTypeDecl(itx.next());
	    }

	if (nfuncDecls != null)
	    synchronized ( nfuncDecls ) {
		Iterator<FunctionDecl> itx = nfuncDecls.iterator();
		while (itx.hasNext())
		    v.visitNFuncDecl(itx.next());
	    }

	if (ufuncDecls != null)
	    synchronized ( ufuncDecls ) {
		Iterator<FunctionDecl> itx = ufuncDecls.iterator();
		while (itx.hasNext())
		    v.visitUFuncDecl(itx.next());
	    }
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder(getClass().getName());
	sb.append("@").append(hashCode()).append("{");
	if (libImportsDecls != null)
	    sb.append("imports=").append(libImportsDecls.size()).append(",");
	if (libExportsDecls != null)
	    sb.append("exports=").append(libExportsDecls.size()).append(",");
	if (globalsDecls != null)
	    sb.append("globals=").append(globalsDecls.size()).append(",");
	if (utypeDecls != null)
	    sb.append("utypes=").append(utypeDecls.size()).append(",");
	if (etypeDecls != null)
	    sb.append("etypes=").append(etypeDecls.size()).append(",");
	if (mtypeDecls != null)
	    sb.append("mtypes=").append(mtypeDecls.size()).append(",");
	if (nfuncDecls != null)
	    sb.append("nfuncs=").append(nfuncDecls.size()).append(",");
	if (ufuncDecls != null)
	    sb.append("ufuncss=").append(ufuncDecls.size());
	sb.append("}");
	return sb.toString();
    }

    /**
     * Find the type declaration object for the type-name provided. The type name object shall either be one of:
     *
     * <ul>
     * <li>One of the static instances of the global types (HANDLE, CODE, BOOLEAN, INTEGER, REAL or STRING);</li>
     * <li>A reference to a type-name singleton for the Chunk file, which was defined by:
     * <ul>
     * <li>a user-type declaration (a type that extends HANDLE, or a type that extends another user-type-name), or</li>
     * <li>from an IMPORTS block declaration type name, whose type-name is represented by a special singleton.</li>
     * </ul>
     * </li>
     * </ul>
     *
     *
     * If the type-name is not one of the six global types; it shall either be a name within:
     *
     * <ul>
     * <li>The local type-name table (<code>utypeDecls</code>); or</li>
     * <li>is a 'synthetic reference' to a yet unresolved type (<code>etypeDecls</code>).</li>
     * </ul>
     *
     *
     * @param name
     *
     * @return
     */
    public TypeDecl findTypeDecl(final String name) {

	/* formal types may not be overridden: */
	if (name.equalsIgnoreCase("integer"))
	    return TypeDecl.TYPE_INTEGER;
	if (name.equalsIgnoreCase("real"))
	    return TypeDecl.TYPE_REAL;
	if (name.equalsIgnoreCase("string"))
	    return TypeDecl.TYPE_STRING;
	if (name.equalsIgnoreCase("boolean"))
	    return TypeDecl.TYPE_BOOLEAN;
	if (name.equalsIgnoreCase("code"))
	    return TypeDecl.TYPE_CODE;
	if (name.equalsIgnoreCase("handle"))
	    return TypeDecl.TYPE_HANDLE;

	/* See if we have already a cached symbol name for the TYPE: */
	if (this.etypeDecls != null)
	    synchronized ( this.etypeDecls ) {
		Iterator<TypeDecl> itx = this.etypeDecls.iterator();
		while (itx.hasNext()) {
		    TypeDecl syntheticTypeRef = itx.next();
		    if (syntheticTypeRef.typeName.equals(name))
			return syntheticTypeRef; // return whatever cached symbol we have
		}
	    }

	/* Check the IMPORTSBLOCKS first to see if there is an import for a TYPE: */
	if (this.libImportsDecls != null)
	    synchronized ( this.libImportsDecls ) {
		Iterator<LibraryBlock> itx = this.libImportsDecls.iterator();
		while (itx.hasNext()) {
		    LibraryBlock importsTableDecl = itx.next();
		    synchronized ( importsTableDecl ) {
			Iterator<LibraryReference> itx1 = importsTableDecl.iterator();
			while (itx1.hasNext()) {
			    LibraryReference importedReference = itx1.next();
			    if (importedReference.type == LibraryReferenceType.TYPE) {
				Iterator<String> itx2 = importedReference.tokens.iterator();
				while (itx2.hasNext()) {
				    String token = itx2.next();
				    if (token.equals(name)) {
					/**
					 * generate a new cached symbol in place of the IMPORTSBLOCK decl; it has priority over whatever
					 * other type(s) might be declared
					 */
					TypeDecl allocSynth = new TypeDecl(TypeDeclType.EXTENDS_IMPORTS);
					allocSynth.typeName = name;
					if (this.etypeDecls == null)
					    this.etypeDecls = new LinkedList<>();
					synchronized ( this.etypeDecls ) {
					    this.etypeDecls.add(allocSynth);
					}
					return allocSynth;
				    }
				}
			    }
			}
		    }
		}
	    }

	/*
	 * no IMPORTSBLOCKS match the name, find the name in the user declarations of
	 * TYPE tokens:
	 */
	if (this.utypeDecls != null)
	    synchronized ( this.utypeDecls ) {
		Iterator<TypeDecl> itx = this.utypeDecls.iterator();
		while (itx.hasNext()) {
		    TypeDecl utypeDecl = itx.next();
		    if (utypeDecl.typeName.equals(name))
			return utypeDecl; // defined by the user
		}
	    }

	/*
	 * no IMPORTSBLOCKS or TYPEDEF sections define the type; give up here.  Since we now resolve
	 * wildcard references, we need a list of type-names we *didnt* resolve (and must still return
	 * a unique singleton).
	 */
	if (this.mtypeDecls != null)
	    synchronized ( this.mtypeDecls ) {
		Iterator<TypeDecl> itx = this.mtypeDecls.iterator();
		while (itx.hasNext()) {
		    TypeDecl syntheticTypeRef = itx.next();
		    if (syntheticTypeRef.typeName.equals(name))
			return syntheticTypeRef; // return whatever cached symbol we have
		}
	    }

	/*
	 * We don't have a cached missing type symbol; create one and return it so we can parse
	 * files with 'missing types' which might be available anyway....:
	 */
	TypeDecl syntheticGenerated = new TypeDecl(TypeDeclType.UNRESOLVED_TYPE);
	syntheticGenerated.typeName = name;

	if (this.mtypeDecls == null)
	    this.mtypeDecls = new LinkedList<>();

	synchronized ( this.mtypeDecls ) {
	    this.mtypeDecls.add(syntheticGenerated);
	}
	return syntheticGenerated;
    }

}
