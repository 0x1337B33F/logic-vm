package logicscript.ast.nodes.globals;

import java.util.LinkedList;
import logicscript.ast.Location;

/**
 * <p>
 * Implements the GLOBALS declaration block node. The globals block node is a list of global variable declarations which are available in
 * the current global scope, or to wider scope(s) when used with EXPORTS block(s).</p>
 *
 * @author Lochie
 */
public class GlobalsBlock extends LinkedList<GlobalsVarDecl> {

    /** the Location where tokenization first occurred: */
    public Location location;
}
