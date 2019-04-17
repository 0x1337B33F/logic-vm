package logicbyte.vm;

import logicbyte.bytecode.LinkerObjectFile.TypesEntry;

/**
 * <p>
 * Contract interface for classes which implement Closure type dictionaries.
 * </p>
 *
 * @author Lochie
 */
public interface VMTypesTable {

    /**
     * Define a new type in the type-table.
     *
     * @param numberOfType The number of the type
     * @param typeDecl     The type declaration
     *
     * @return If the type was defined or not
     */
    public boolean defineType(int numberOfType, TypesEntry typeDecl);

    /**
     * Get the type declaration for a type number
     *
     * @param typeNumber The type number
     *
     * @return The type declaration
     */
    public TypesEntry getTypeOf(int typeNumber);

    /**
     * Get the number of a type declaration
     *
     * @param typeDecl The type declaration
     *
     * @return The type number
     */
    public int getNumberOf(TypesEntry typeDecl);

    /**
     * Get the number of a type name
     *
     * @param typeName The type name
     *
     * @return The type number
     */
    public int getNumberOf(String typeName);

}
