package logicscript.ast.nodes.decl;

import logicscript.ast.ChunkVisitor;
import logicscript.ast.Location;
import logicscript.ast.Visitable;
import logicscript.ast.nodes.expr.Expression;

/**
 * <p>
 * Implements the base function local variable declaration type. The local variable declaration type is used to create a closure for
 * user-defined functions (<code>FUNCTION</code>, FunctionDeclType.USER).</p>
 *
 * @author Lochie
 */
public class FunctionLVarDecl implements Visitable {

    /**
     * The name of the local variable
     */
    public String nameOfLocalVar;

    /**
     * The type of the local variable
     */
    public TypeDecl valueType;

    /**
     * If the local variable is an array
     */
    public boolean isArrayType;

    /**
     * The initialization expression of the variable, or null if no initexpr
     */
    public Expression initialRhs;

    /**
     * the Location where tokenization first began
     */
    public Location location;

    /**
     * Default constructor
     */
    public FunctionLVarDecl() {
    }

    /**
     * @param name The name of the lvar
     * @param type The type of the lvar
     */
    public FunctionLVarDecl(String name, TypeDecl type) {
	this(name, type, null);
    }

    /**
     * @param name The name of the lvar
     * @param type The type of the lvar
     * @param rhs  The right-hand-side initialization expression
     */
    public FunctionLVarDecl(String name, TypeDecl type, Expression rhs) {
	this(name, type, rhs, false);
    }

    /**
     * @param name          The name of the lvar
     * @param type          The type of the lvar
     * @param rhsOrArrayDim The right-hand-side initialization expression, or the array dimension as an Integer
     * @param isArrayType   If the type of the lvar is an array of the type <code>type[]</code>
     */
    public FunctionLVarDecl(String name, TypeDecl type, Expression rhsOrArrayDim, boolean isArrayType) {
	this.nameOfLocalVar = name;
	this.valueType = type;
	this.initialRhs = rhsOrArrayDim;
	this.isArrayType = isArrayType;
    }

    @Override
    public void visit(ChunkVisitor v) throws Exception {
	if (initialRhs != null)
	    v.visitExpression(initialRhs);
    }
}
