package logicscript.ast.nodes.decl;

import logicscript.ast.Location;

/**
 * <p>
 * Abstract class used to ancestor all AST nodes which are of the data-type Declaration; declarations are scoped element(s) which define
 * either:</p>
 *
 * <ul>
 * <li>A type name (TypeDecl);</li>
 * <li>A function declaration (FunctionDecl); or</li>
 * <li>A function local variable declaration (FunctionLVarDecl)</li>
 * </ul>
 *
 * <p>
 * A declaration node may not appear within:</p>
 * <ul>
 * <li>any Statement or Expression;</li>
 * <li>any Block; or </li>
 * <li>any IMPORTS or EXPORTS section(s)</li>
 * </ul>
 *
 *
 *
 * @author Lochie
 */
public class Decl {

    /** the Location where tokenization first occurred: */
    public Location location;
}
