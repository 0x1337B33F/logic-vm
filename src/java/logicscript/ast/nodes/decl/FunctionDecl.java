package logicscript.ast.nodes.decl;

import java.util.Iterator;
import java.util.List;
import logicscript.ast.ChunkVisitor;
import logicscript.ast.Visitable;
import logicscript.ast.nodes.stmt.Block;

/**
 * <p>
 * Implements the base function declaration block type. The function declaration block contains:
 * </p>
 *
 * <ul>
 * <li>The type of the declaration (native function, user function);</li>
 * <li>The list of the parameter names, if defined;</li>
 * <li>The list of the parameter types, if defined;</li>
 * <li>The return type of the declaration, or <code>nothing</code>;</li>
 * <li>A list of local variable declarations, if defined; and</li>
 * <li>A pointer to the function's top-level {@link Block} object.</li>
 * </ul>
 *
 * <p>
 * Depending on the type of function declaration, a number of the fields may be set or null.</p>
 *
 *
 * @author Lochie
 */
public class FunctionDecl extends Decl implements Visitable {

    /**
     * Indicates the function declaration type
     */
    public static enum FunctionDeclType {

	/**
	 * The function declaration is for a <code>NATIVE FUNCTION</code> declaration; a native function declaration is a reference to a
	 * Java `function' which is not a compile-able symbol or reference, but is available for invocation. The specific `function'
	 * receiver must specifically be configured to accept invocation by compiled object code.
	 */
	NATIVE,
	/**
	 * The function declaration is for a <code>FUNCTION</code> declaration; a user function declaration is a declaration which contains
	 * local variables &amp; a top-level {@link Block}.
	 */
	USER;
    }

    /**
     * The type of the function declaration
     */
    public final FunctionDeclType type;

    /**
     * The name of the function symbol
     */
    public String name;

    /**
     * The list of parameter names, or null if no params
     */
    public List<String> paramNames;

    /**
     * The list of parameter types, or null if no params
     */
    public List<TypeDecl> paramTypes;

    /**
     * The return type or <code>nothing</code>
     */
    public TypeDecl returnType;

    /**
     * The list of local variables, if defined (&amp; type == USER)
     */
    public List<FunctionLVarDecl> lvars;

    /**
     * The pointer to the top-level statement block, if defined (&amp; type == USER)
     */
    public Block block;

    /**
     * Default constructor
     *
     * @param type The type of the declaration, immutable
     */
    public FunctionDecl(FunctionDeclType type) {
	this.type = type;
    }

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (lvars != null)
	    synchronized ( lvars ) {
		Iterator<FunctionLVarDecl> itx = lvars.iterator();
		while (itx.hasNext())
		    v.visitFuncDeclLVar(itx.next());
	    }

	if (block != null)
	    v.visitFuncDeclBlock(block);
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder(getClass().getName());
	sb.append("@").append(hashCode()).append("{");
	sb.append("type=").append(type).append(",");
	sb.append("name=").append(name).append(",");
	if (paramTypes != null)
	    sb.append("numparams=").append(paramTypes.size()).append(",");
	if (returnType != null)
	    sb.append("returns=").append(returnType.toString()).append(",");
	if (lvars != null)
	    sb.append("numlocals=").append(lvars.size()).append(",");
	if (block != null)
	    sb.append("block=").append(block.size());
	sb.append("}");
	return sb.toString();
    }

}
