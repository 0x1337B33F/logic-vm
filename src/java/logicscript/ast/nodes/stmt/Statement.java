package logicscript.ast.nodes.stmt;

import logicscript.ast.Location;
import logicscript.ast.Visitable;

/**
 * <p>
 * Implements the Statement base node type.
 * </p>
 *
 * @author Lochie
 */
public abstract class Statement implements Visitable {

    /** the Location where tokenization first occurred: */
    public Location location;
}
