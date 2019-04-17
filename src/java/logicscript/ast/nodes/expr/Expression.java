package logicscript.ast.nodes.expr;

import logicscript.ast.Location;
import logicscript.ast.Visitable;

/**
 * <p>
 * Abstract class used to ancestor all AST nodes which are of the data-type Expression; Expression is a generic abstraction for any
 * expression component. Expression component(s) include:
 * </p>
 *
 * <ul>
 *
 * <li>Value objects : those component(s) which are either implicit values or references to values:
 * <ul>
 * <li>VariableRefExpression, VariableArrayRefExpression - an expression which references the literal value contained within a
 * variable;</li>
 * <li>FunctionCallExpression, FunctionCallVarExpression - an expression whose value is the literal RESULT value of the function call;
 * or</li>
 * <li>LiteralExpression - a literal value.</li>
 * </ul>
 * </li>
 *
 * <li>Compound objects : those component(s) which are compound (nested expressional) structures:
 * <ul>
 * <li>UnaryExpression - an expression which operates with one nested (right-hand side) expression;</li>
 * <li>BinaryExpression - an expression which operates with two nested (left- &amp; right-hand side) expressions;</li>
 * <li>ParenExpression - an expression which is contained within a set of parenthesis;</li>
 * </ul>
 * </li>
 *
 * </ul>
 *
 * <p>
 * Expressions are parsed lexically left-to-right; an Expression is either:</p>
 * <ul>
 * <li>A single value object, or;</li>
 * <li>A single compound object, which is the root of the Expression tree</li>
 * </ul>
 *
 * <p>
 * When represented within the AST tree, the root node of the Expression is the right-most EXPR OPER EXPR grouping of the binary expression
 * chain, or the single term (if it is a unary or value object).</p>
 *
 * <p>
 * Future expressional operand(s) may include:
 * </p>
 *
 * <ul>
 * <li>TernaryExpression - an expression which operates with three nested (left-, middle- &amp; right-hand side) expressions;</li>
 * </ul>
 *
 *
 *
 * @author Lochie
 */
public abstract class Expression implements Visitable {

    /** the Location where tokenization first occurred: */
    public Location location;

}
