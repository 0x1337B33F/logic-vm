package logicbyte.vm.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import logicbyte.vm.VMClosureTypeMapper;
import logicbyte.vm.VMType;
import logicbyte.vm.exception.DuplicateTypeNameException;
import logicbyte.vm.exception.DuplicateTypeNumberException;
import logicbyte.vm.exception.NoSuchTypeException;
import logictype.BiDiMap;

/**
 *
 * @author Lochie
 */
public class BaseVMClosureTypeMapper implements VMClosureTypeMapper {

    public static final int TYPENUMBER_ANY = -1;
    public static final int TYPENUMBER_NOTHING = 0;
    public static final int TYPENUMBER_HANDLE = 1;
    public static final int TYPENUMBER_CODE = 2;
    public static final int TYPENUMBER_BOOLEAN = 3;
    public static final int TYPENUMBER_INTEGER = 4;
    public static final int TYPENUMBER_REAL = 5;
    public static final int TYPENUMBER_STRING = 6;

    public static final int TYPEINDEX_USER = 7;

    /**
     * The mapping of local type numbers to virtual machine types.
     */
    final BiDiMap<Integer, VMType> typeTable;

    /**
     * The mapping of type declarations.
     */
    final HashMap<Integer, Boolean> definesTypeTable;

    public BaseVMClosureTypeMapper() {
	this.typeTable = new BiDiMap<>();
	this.definesTypeTable = new HashMap<>();
    }

    @Override
    public boolean defineType(VMType type, int numberOfType, boolean definesType) throws DuplicateTypeNumberException, DuplicateTypeNameException {
	if (typeTable.containsKey(numberOfType))
	    throw new DuplicateTypeNumberException(numberOfType);
	if (typeTable.reverse().containsKey(type))
	    throw new DuplicateTypeNameException(type.name());
	typeTable.put(numberOfType, type);
	definesTypeTable.put(numberOfType, definesType);
	return true;

    }

    @Override
    public VMType getTypeOf(int typeNumber) throws NoSuchTypeException {
	if (typeNumber < TYPEINDEX_USER)
	    switch (typeNumber) {
		case -1:
		    return BaseVMPrimitiveType.TYPE_ANY;
		case 0:
		    return BaseVMPrimitiveType.TYPE_NOTHING;
		case 1:
		    return BaseVMPrimitiveType.TYPE_HANDLE;
		case 2:
		    return BaseVMPrimitiveType.TYPE_CODE;
		case 3:
		    return BaseVMPrimitiveType.TYPE_BOOLEAN;
		case 4:
		    return BaseVMPrimitiveType.TYPE_INTEGER;
		case 5:
		    return BaseVMPrimitiveType.TYPE_REAL;
		case 6:
		    return BaseVMPrimitiveType.TYPE_STRING;
	    }
	VMType result = typeTable.get(typeNumber);
	if (result == null)
	    throw new NoSuchTypeException(typeNumber);
	return result;
    }

    @Override
    public VMType getTypeOf(String typeName) throws NoSuchTypeException {
	Iterator<VMType> itx = typeTable.values().iterator();
	while (itx.hasNext()) {
	    VMType type = itx.next();
	    if (type.name().equals(typeName))
		return type;
	}
	throw new NoSuchTypeException(typeName);
    }

    @Override
    public LinkedList<VMType> getTypes() {
	LinkedList<VMType> list = new LinkedList<>();
	list.addAll(typeTable.values());
	return list;
    }

    @Override
    public int getNumberOf(VMType type) throws NoSuchTypeException {
	Integer key = typeTable.reverse().get(type);
	if (key != null)
	    return (int) key;
	throw new NoSuchTypeException(type.name());
    }

    @Override
    public boolean definesType(int typeNumber) throws NoSuchTypeException {
	if (typeNumber < TYPEINDEX_USER)
	    return false;
	return definesTypeTable.get(typeNumber);
    }

    @Override
    public boolean definesType(VMType type) throws NoSuchTypeException {
	return definesType(getNumberOf(type));
    }

}
