package logicbyte.vm;

import java.util.LinkedList;
import logicbyte.vm.exception.DuplicateTypeNameException;
import logicbyte.vm.exception.DuplicateTypeNumberException;
import logicbyte.vm.exception.NoSuchTypeException;

/**
 * <p>
 * Contract interface for classes which implement Closure type dictionaries.
 * </p>
 *
 * @author Lochie
 */
public interface VMClosureTypeMapper {

    /**
     * <p>
     * Define a type in the type-mapper; the type-number associated with the type reference shall be used when looking up the real type of
     * the type-number in the bytecode.</p>
     *
     * @param typeDecl     The type declaration instance
     * @param numberOfType The number of the type-mapping
     * @param definesType  If the closure defines the type explicitly
     *
     * @return If the type was defined
     *
     * @throws DuplicateTypeNumberException If the type-number has already been assigned to a type reference, a
     *                                      {@link DuplicateTypeNumberException} will be thrown.
     * @throws DuplicateTypeNameException   If the type reference has already been assigned to a type-number, a
     *                                      {@link DuplicateTypeNameException} will be thrown.
     */
    public boolean defineType(VMType typeDecl, int numberOfType, boolean definesType) throws DuplicateTypeNumberException, DuplicateTypeNameException;

    /**
     * <p>
     * Convert a type-number to a type reference.</p>
     *
     * @param typeNumber The type-number, bytecode
     *
     * @return The type reference
     *
     * @throws NoSuchTypeException If the type-number is not known, a {@link NoSuchTypeException} will be thrown.
     */
    public VMType getTypeOf(int typeNumber) throws NoSuchTypeException;

    /**
     * <p>
     * Get the definition of a type by type-name; returns the locally known type definition reference for the type-name.</p>
     *
     * @param typeName The type-name
     *
     * @return The known definition of the type by reference
     *
     * @throws NoSuchTypeException If the type name is not known, a {@link NoSuchTypeException} will be thrown.
     */
    public VMType getTypeOf(String typeName) throws NoSuchTypeException;

    /**
     * <p>
     * Get a List of all known types in this mapper.</p>
     *
     * @return All registered types in this mapper
     */
    public LinkedList<VMType> getTypes();

    /**
     * <p>
     * Determine if this type-mapper defines a type-number.</p>
     *
     * @param typeNumber The type-number
     *
     * @return If a type is registered with the type-number
     *
     * @throws NoSuchTypeException
     */
    public boolean definesType(int typeNumber) throws NoSuchTypeException;

    /**
     * <p>
     * Get the number assigned to a type reference.</p>
     *
     * @param type The type reference
     *
     * @return The type-number
     *
     * @throws NoSuchTypeException If the type is not known, a {@link NoSuchTypeException} will be thrown.
     */
    public int getNumberOf(VMType type) throws NoSuchTypeException;

    /**
     * <p>
     * Determine if this type-mapper is the declarative location of the type-reference provided. Specifically, this method tests whether the
     * reference provided is defined by the Closure which is associated to the mapper.</p>
     *
     * @param type The type reference
     *
     * @return If this type-reference is declared by this mapper
     *
     * @throws NoSuchTypeException If the type is not known, a {@link NoSuchTypeException} will be thrown.
     */
    public boolean definesType(VMType type) throws NoSuchTypeException;

}
