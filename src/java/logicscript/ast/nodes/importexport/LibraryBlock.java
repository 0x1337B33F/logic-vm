package logicscript.ast.nodes.importexport;

import java.util.LinkedList;
import logicscript.ast.Location;

/**
 *
 * <p>
 * Implements the IMPORTS/EXPORTS declaration block nodes. The IMPORTS/EXPORTS node type(s) allow symbols of types GLOBAL, FUNCTION and TYPE
 * to be exchanged between files using unqualified &amp; unqualified name(s), whose symbols shall be linked at final compilation time or by
 * dynamically loading required symbol(s).</p>
 *
 *
 * @author Lochie
 */
public class LibraryBlock extends LinkedList<LibraryReference> {

    public static enum BlockType {
	IMPORTS, EXPORTS;
    }

    /**
     * The type of the block, IMPORTS or EXPORTS:
     */
    public final BlockType type;

    /**
     * The qualified / unqualified symbol group name
     */
    public String symbol;

    /** the Location where tokenization first occurred: */
    public Location location;

    public LibraryBlock(BlockType type) {
	this.type = type;
    }

}
