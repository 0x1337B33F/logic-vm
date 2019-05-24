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

    public boolean defineType(VMType typeDecl, int numberOfType, boolean definesType) throws DuplicateTypeNumberException, DuplicateTypeNameException;

    public VMType getTypeOf(int typeNumber) throws NoSuchTypeException;

    public VMType getTypeOf(String typeName) throws NoSuchTypeException;

    public LinkedList<VMType> getTypes();

    public boolean definesType(int typeNumber) throws NoSuchTypeException;

    public int getNumberOf(VMType type) throws NoSuchTypeException;

    public boolean definesType(VMType type) throws NoSuchTypeException;

}
