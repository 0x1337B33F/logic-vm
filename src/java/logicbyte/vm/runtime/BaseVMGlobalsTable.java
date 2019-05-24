package logicbyte.vm.runtime;

import java.util.HashMap;
import java.util.Map;
import logicbyte.vm.VMGlobalsTable;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public class BaseVMGlobalsTable implements VMGlobalsTable {

    final Map<String, GlobalBox> globalBoxMap;

    public BaseVMGlobalsTable() {
	this.globalBoxMap = new HashMap<>();
    }

    @Override
    public boolean defineGlobal(String nameOfGlobalValue, GlobalType typeOfGlobal, VMType typeOfGlobalValue, boolean globalIsArrayType,
	    Object initialGlobalValueObject) {
	if (globalBoxMap.containsKey(nameOfGlobalValue))
	    return false;
	GlobalBox allocBox = new GlobalBox();
	allocBox.typeOfGlobal = typeOfGlobal;
	allocBox.typeOfGlobalValue = typeOfGlobalValue;
	allocBox.globalIsArrayType = globalIsArrayType;
	allocBox.valueInGlobalBox = initialGlobalValueObject;
	globalBoxMap.put(nameOfGlobalValue, allocBox);
	return true;
    }

    @Override
    public GlobalBox getGlobal(String nameOfGlobalValue) {
	return globalBoxMap.get(nameOfGlobalValue);
    }

}
