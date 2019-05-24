package logicbyte.vm.runtime;

import logicbyte.vm.VMType;

public class BaseVMArrayImpl {

    public final VMType typeOfArray;
    public final int sizeOfArray;
    public final Object[] arrayValues;

    public BaseVMArrayImpl(VMType typeOfArray, int sizeOfArray) {
	this.typeOfArray = typeOfArray;
	this.sizeOfArray = sizeOfArray;
	this.arrayValues = new Object[ this.sizeOfArray ];
    }

}
