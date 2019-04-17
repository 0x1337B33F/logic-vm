package logicbyte.vm.runtime.func;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.runtime.BaseVMJFunctionImpl.VMJFunctionLabel;

/**
 *
 * @author Lochie
 */
public class BaseFuncs {

    @VMJFunctionLabel(name = "TypeOf")
    public static String getTypeOf(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return vm.getJTypeName(args[0].getClass());
    }

    @VMJFunctionLabel(name = "InstanceOf")
    public static boolean getInstanceOf(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return vm.isJType(args[0].getClass(), (String) args[1]);
    }

    @VMJFunctionLabel(name = "Cast_I2S")
    public static String castFromInteger(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return Integer.toString((Integer) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_R2S")
    public static String castFromReal(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return Double.toString((Double) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_B2S")
    public static String castFromBoolean(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return Boolean.toString((Boolean) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_S2I")
    public static int castToInteger(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return Integer.parseInt((String) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_S2R")
    public static double castToReal(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return Double.parseDouble((String) args[0]);
    }

    @VMJFunctionLabel(name = "Cast_S2B")
    public static boolean castToBoolean(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return Boolean.parseBoolean((String) args[0]);
    }

    @VMJFunctionLabel(name = "Str_IndexOf")
    public static int stringIndexOf(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return ((String) args[0]).indexOf((String) args[1], (Integer) args[2]);
    }

    @VMJFunctionLabel(name = "Str_SubStr")
    public static String stringSubstring(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	int min = (Integer) args[1], max = (Integer) args[2];
	if (max == 0)
	    return ((String) args[0]).substring(min);
	else if (max < 0)
	    return ((String) args[0]).substring(min + max, min);
	else
	    return ((String) args[0]).substring(min, max);
    }

    @VMJFunctionLabel(name = "Str_Equals")
    public static boolean stringEquals(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	if ((Boolean) args[2])
	    return ((String) args[0]).equalsIgnoreCase((String) args[1]);
	return ((String) args[0]).equals((String) args[1]);
    }

    @VMJFunctionLabel(name = "Str_Concat")
    public static String stringConcat(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) {
	return ((String) args[0]) + ((String) args[1]);
    }

}
