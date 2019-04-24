package logicbyte.vm.runtime;

import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicbyte.vm.VMTypesTable;
import logictype.BiDiMap;

/**
 *
 * @author Lochie
 */
public class BaseVMTypesTable implements VMTypesTable {

    public static final int TYPENUMBER_NOTHING = 0;
    public static final int TYPENUMBER_HANDLE = 1;
    public static final int TYPENUMBER_CODE = 2;
    public static final int TYPENUMBER_BOOLEAN = 3;
    public static final int TYPENUMBER_INTEGER = 4;
    public static final int TYPENUMBER_REAL = 5;
    public static final int TYPENUMBER_STRING = 6;

    final BiDiMap<Integer, String> typeTable;

    public BaseVMTypesTable() {
	this.typeTable = new BiDiMap<>();
    }

    public boolean defineType(int numberOfType, String typeDecl) {
	if (typeTable.containsKey(numberOfType))
	    return false;
	typeTable.put(numberOfType, typeDecl);
	return true;
    }

    public String getTypeOf(int typeNumber) {
	return typeTable.get(typeNumber);
    }

    public int getNumberOf(TypesEntry typeDecl) {
	return typeTable.reverse().get(typeDecl);
    }

    public int getNumberOf(String typeName) {
	Integer key = typeTable.reverse().get(typeName);
	if (key != null)
	    return (int) key;
	return -1;
    }

}
