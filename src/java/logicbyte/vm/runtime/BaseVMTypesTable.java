package logicbyte.vm.runtime;

import java.util.Iterator;
import logicbyte.bytecode.LinkerObjectFile.TypesEntry;
import logicbyte.vm.VMTypesTable;
import logictype.BiDiMap;

/**
 *
 * @author Lochie
 */
public class BaseVMTypesTable implements VMTypesTable {

    final BiDiMap<Integer, TypesEntry> typeTable;

    public BaseVMTypesTable() {
	this.typeTable = new BiDiMap<>();
    }

    public boolean defineType(int numberOfType, TypesEntry typeDecl) {
	if (typeTable.containsKey(numberOfType))
	    return false;
	typeTable.put(numberOfType, typeDecl);
	return true;
    }

    public TypesEntry getTypeOf(int typeNumber) {
	return typeTable.get(typeNumber);
    }

    public int getNumberOf(TypesEntry typeDecl) {
	return typeTable.reverse().get(typeDecl);
    }

    public int getNumberOf(String typeName) {
	Iterator<TypesEntry> itx = this.typeTable.values().iterator();
	while (itx.hasNext()) {
	    TypesEntry e = itx.next();
	    if (e.name.equals(typeName))
		return e.slot;
	}
	return -1;
    }

}
