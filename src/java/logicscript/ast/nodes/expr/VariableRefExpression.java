package logicscript.ast.nodes.expr;

import logicscript.ast.ChunkVisitor;

/**
 * <p>
 * Implementation of a variable reference expression; the return type of the expression becomes the type of the local variable, global
 * variable or imported global variable. The value of the variable may not be specifically consistent with the initialization expression of
 * the variable; the real value of the variable shall not be known until execution.</p>
 *
 * <p>
 * The local, global or imported global must exist in the appropriate scope.</p>
 *
 * @author Lochie
 */
public class VariableRefExpression extends Expression {

    public String nameOfLocalOrGlobalVar;

    @Override
    public void visit(ChunkVisitor v) {
    }

}
