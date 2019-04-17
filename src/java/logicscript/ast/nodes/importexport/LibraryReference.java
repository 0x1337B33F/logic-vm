package logicscript.ast.nodes.importexport;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * Implements a reference to a symbol being IMPORTed or EXPORTed by an IMPORTS or EXPORTS block.
 * </p>
 *
 * @author Lochie
 */
public class LibraryReference {

    /**
     * Indicates the type of the reference:
     */
    public static enum LibraryReferenceType {
	/**
	 * The symbol or wildcard match refers to TYPES
	 */
	TYPE,
	/**
	 * The symbol or wildcard match refers to FUNCTION
	 */
	FUNCTION,
	/**
	 * The symbol or wildcard match refers to GLOBAL
	 */
	GLOBAL
    }

    /**
     * The list of symbol(s) in the reference:
     */
    public List<String> tokens = new LinkedList<>();

    /**
     * If the reference shall mean 'all of type'
     */
    public boolean isWildcard = false;

    /**
     * The type of the reference
     */
    public final LibraryReferenceType type;

    /**
     * Default constructor
     *
     * @param type The type of the reference, immutable
     */
    public LibraryReference(LibraryReferenceType type) {
	this.type = type;
    }

}
