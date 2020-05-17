package logicbyte.vm.runtime.func;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMFunction;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.exception.ThrowObjectException;
import logicbyte.vm.exception.VMException;
import logicbyte.vm.runtime.BaseVMArrayImpl;
import logicbyte.vm.runtime.BaseVMJFunctionImpl.VMJFunctionLabel;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public class BaseFuncs {

    @VMJFunctionLabel(name = "TypeOf")
    public static String getTypeOf(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return vm.getJType(caller.closure(), args[0].getClass()).name();
    }

    @VMJFunctionLabel(name = "InstanceOf")
    public static boolean getInstanceOf(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) throws VMException {
	VMType castToType = vm.getType(caller.closure(), (String) args[1]);
	return vm.canCastToType(castToType, vm.getJType(caller.closure(), args[0].getClass()));
    }

    @VMJFunctionLabel(name = "SizeOf")
    public static int getSizeOf(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) throws ThrowObjectException {
	if (!(args[0] instanceof BaseVMArrayImpl))
	    throw new ThrowObjectException("argument 1 not array");
	return ((BaseVMArrayImpl) args[0]).sizeOfArray;
    }

    @VMJFunctionLabel(name = "Cast_I2S")
    public static String castFromInteger(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Integer.toString((Integer) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_R2S")
    public static String castFromReal(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Double.toString((Double) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_B2S")
    public static String castFromBoolean(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Boolean.toString((Boolean) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_L2S")
    public static String castFromLong(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Long.toString((Long) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_S2I")
    public static int castToInteger(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Integer.parseInt((String) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_S2R")
    public static double castToReal(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Double.parseDouble((String) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_S2B")
    public static boolean castToBoolean(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Boolean.parseBoolean((String) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_S2L")
    public static long castToLong(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return Long.parseLong((String) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_I2L")
    public static long castIntegerToLong(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return ((Integer) args[0]).longValue();
    }

    @VMJFunctionLabel(name = "Cast_R2L")
    public static long castRealToLong(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return ((Double) args[0]).longValue();
    }

    @VMJFunctionLabel(name = "Cast_L2I")
    public static int castLongToIntegerWorded(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	Long l0 = (Long) args[0];
	if ((boolean) args[1])
	    return (Integer) ((Long) (Long.rotateRight(l0, 32) & (long) 0x00000000FFFFFFFF)).intValue();
	else
	    return (Integer) ((Long) (l0 & (long) 0x00000000FFFFFFFF)).intValue();
    }

    @VMJFunctionLabel(name = "Cast_L2R")
    public static double castLongToRealWorded(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	Long l0 = (Long) args[0];
	if ((boolean) args[1])
	    return (Double) ((Long) (Long.rotateRight(l0, 32) & (long) 0x00000000FFFFFFFF)).doubleValue();
	else
	    return (Double) ((Long) (l0 & (long) 0x00000000FFFFFFFF)).doubleValue();
    }

    @VMJFunctionLabel(name = "Str_IndexOf")
    public static int stringIndexOf(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return ((String) args[0]).indexOf((String) args[1], (Integer) args[2]);
    }

    @VMJFunctionLabel(name = "Str_SubStr")
    public static String stringSubstring(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	int min = (Integer) args[1], max = (Integer) args[2];
	if (max == 0)
	    return ((String) args[0]).substring(min);
	else if (max < 0)
	    return ((String) args[0]).substring(min + max, min);
	else
	    return ((String) args[0]).substring(min, max);
    }

    @VMJFunctionLabel(name = "Str_Equals")
    public static boolean stringEquals(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	if ((Boolean) args[2])
	    return ((String) args[0]).equalsIgnoreCase((String) args[1]);
	return ((String) args[0]).equals((String) args[1]);
    }

    @VMJFunctionLabel(name = "Str_Concat")
    public static String stringConcat(LogicVM vm, VMInvokable executor, VMFunction caller, VMDebugCallback debug, Object[] args) {
	return ((String) args[0]) + ((String) args[1]);
    }

}
