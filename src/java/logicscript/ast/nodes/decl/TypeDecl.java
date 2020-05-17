package logicscript.ast.nodes.decl;

/**
 *
 * @author Lochie
 */
public class TypeDecl extends Decl {

    public static final TypeDecl TYPE_INTEGER = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "integer");
    public static final TypeDecl TYPE_REAL = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "real");
    public static final TypeDecl TYPE_BOOLEAN = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "boolean");
    public static final TypeDecl TYPE_STRING = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "string");
    public static final TypeDecl TYPE_CODE = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "code");
    public static final TypeDecl TYPE_HANDLE = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "handle");
    public static final TypeDecl TYPE_LONG = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "long");

    public static final TypeDecl TYPE_NOTHING = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "nothing");
    public static final TypeDecl TYPE_NULL = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "null");

    public static final TypeDecl TYPE_ANY = new TypeDecl(TypeDeclType.SYSTEM_TYPE, "any");

    public static enum TypeDeclType {

	/**
	 * Indicates the type is a user-defined type that extends the HANDLE type implicitly; it is a first-level abstraction of the HANDLE
	 * type.
	 */
	EXTENDS_HANDLE,
	/**
	 * Indicates the type is a user-defined type that extends a named type; it is an nth-level abstraction of the HANDLE type.
	 */
	EXTENDS_TYPENAME,
	/**
	 * Indicates the type is a type-name being IMPORTed by an IMPORTS block; the type-name may be either a first- or nth-level
	 * abstraction (extends HANDLE / extends TYPENAME).
	 */
	EXTENDS_IMPORTS,
	/**
	 * Indicates the type is a missing type which the parser has created in order to allow resolution of type-symbols by wildcard
	 * expression; it is not known whether the type exists or is available yet.
	 */
	UNRESOLVED_TYPE,
	/**
	 * Indicates the type is defined by the system (within JVM). The type gains certain immutability, since it is represented in the AST
	 * as a static singleton (to reduce memory consumption).
	 */
	SYSTEM_TYPE
    }

    /**
     * The type of the type
     */
    public final TypeDeclType type;

    /**
     * The name of the type
     */
    public String typeName;
    /**
     * The ancestor type-name of the type, if the type is an nth-level abstraction
     */
    public String ancestorTypeName;

    /**
     * Internal
     *
     * @param type     The type of the declaration, immutable
     * @param typeName The initial type name value
     */
    TypeDecl(TypeDeclType type, String typeName) {
	this.type = type;
	this.typeName = typeName;
    }

    /**
     * Default constructor.
     *
     * @param type The type of the declaration, immutable.
     */
    public TypeDecl(TypeDeclType type) {
	this.type = type;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("TypeDecl[");
	sb.append("type=").append(this.type);
	sb.append(",name=").append(this.typeName);
	if (this.ancestorTypeName != null)
	    sb.append(",ancestorType=").append(this.ancestorTypeName);
	return sb.append("]").toString();
    }

}
