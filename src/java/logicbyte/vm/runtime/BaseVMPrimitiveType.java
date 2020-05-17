package logicbyte.vm.runtime;

import logicbyte.vm.VMClosure;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public class BaseVMPrimitiveType implements VMType {

    public static final BaseVMPrimitiveType TYPE_ANY = new BaseVMPrimitiveType("any");
    public static final BaseVMPrimitiveType TYPE_NOTHING = new BaseVMPrimitiveType("nothing");
    public static final BaseVMPrimitiveType TYPE_HANDLE = new BaseVMPrimitiveType("handle");
    public static final BaseVMPrimitiveType TYPE_CODE = new BaseVMPrimitiveType("code");
    public static final BaseVMPrimitiveType TYPE_BOOLEAN = new BaseVMPrimitiveType("boolean");
    public static final BaseVMPrimitiveType TYPE_INTEGER = new BaseVMPrimitiveType("integer");
    public static final BaseVMPrimitiveType TYPE_REAL = new BaseVMPrimitiveType("real");
    public static final BaseVMPrimitiveType TYPE_LONG = new BaseVMPrimitiveType("long");
    public static final BaseVMPrimitiveType TYPE_STRING = new BaseVMPrimitiveType("string");

    private final String internalName;

    private BaseVMPrimitiveType(final String internalName) {
	this.internalName = internalName;
    }

    @Override
    public String name() {
	return this.internalName;
    }

    @Override
    public VMClosure closure() {
	// FIXME: It isn't defined by a closure. What to do here?
	throw new UnsupportedOperationException("lol nope");
    }

    @Override
    public VMType supertype() {
	return null;
    }

    @Override
    public Class<?> jklazz() {
	return null;
    }

    @Override
    public String toString() {
	return new StringBuilder("VMPrimitiveType{ type=").append(internalName).append(" }").toString();
    }

}
