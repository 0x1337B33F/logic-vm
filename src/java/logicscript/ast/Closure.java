package logicscript.ast;

import logicscript.ast.nodes.decl.TypeDecl;

/**
 *
 * @author Lochie
 */
public interface Closure {

    /**
     * Returns if the closure has at least one closure variable defined
     */
    public boolean hascvars();

    /**
     * The number of closure variables
     */
    public int numcvars();

    /**
     * Get the name of a closure variable in a slot [0, numcvars()}:
     */
    public String getcvarname(int which);

    /**
     * Get the type of a closure variable in a slot [0, numcvars()}:
     */
    public TypeDecl getcvartype(int which);

}
