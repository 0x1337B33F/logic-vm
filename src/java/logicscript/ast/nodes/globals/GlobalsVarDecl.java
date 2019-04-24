package logicscript.ast.nodes.globals;

import logicscript.ast.nodes.decl.TypeDecl;
import logicscript.ast.nodes.expr.Expression;

/**
 *
 * <p>
 * Implements a global variable declaration entry.</p>
 *
 * @author Lochie
 */
public class GlobalsVarDecl {

    /**
     * The name of the global variable symbol
     */
    public String nameOfGlobalVar;

    /**
     * The type of the global variable symbol
     */
    public TypeDecl valueType;

    /**
     * If the global variable declaration is an array or not
     */
    public boolean isArrayType;

    /**
     * The initialization expression, or null if no initexpr
     */
    public Expression rhs;

}
