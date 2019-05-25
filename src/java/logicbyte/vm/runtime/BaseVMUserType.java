package logicbyte.vm.runtime;

import logicbyte.vm.LogicVM;
import logicbyte.vm.VMClosure;
import logicbyte.vm.exception.IllegalTypeMutationException;
import logicbyte.vm.VMType;

/**
 *
 * @author Lochie
 */
public class BaseVMUserType implements VMType {

    private final VMClosure declaringClosure;

    private final String nameOfType;

    private final VMType ancestorOfType;

    private Class<?> klazzOfType;

    public BaseVMUserType(final VMClosure declaringClosure, final String nameOfType) {
	this(declaringClosure, nameOfType, null);
    }

    public BaseVMUserType(final VMClosure declaringClosure, final String nameOfType, final VMType ancestorOfType) {
	this.declaringClosure = declaringClosure;
	this.nameOfType = nameOfType;
	this.ancestorOfType = ancestorOfType;
    }

    protected void setKlazzType(final LogicVM vm, final Class<?> newKlazzOfType) throws IllegalTypeMutationException {
	if (this.klazzOfType != null)
	    throw new IllegalTypeMutationException(vm, this, "cannot reassign class type");

	if (this.ancestorOfType != null) {
	    VMType rootType = this.ancestorOfType;
	    while (rootType != null && rootType.jklazz() == null)
		rootType = rootType.supertype();

	    if (rootType != null) {
		Class<?> klazzOfSuper = rootType.jklazz();
		if (klazzOfSuper != null && !klazzOfSuper.isAssignableFrom(newKlazzOfType))
		    throw new IllegalTypeMutationException(vm, this, String.format("cannot assign class `%s` to class `%s` by widening",
			    newKlazzOfType.getName(), klazzOfSuper.getName()));
	    }
	}

	this.klazzOfType = newKlazzOfType;
    }

    @Override
    public String name() {
	return this.nameOfType;
    }

    @Override
    public VMClosure closure() {
	return this.declaringClosure;
    }

    @Override
    public VMType supertype() {
	return this.ancestorOfType;
    }

    @Override
    public Class<?> jklazz() {
	return klazzOfType;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder("VMType{ type=").append(nameOfType);
	if (klazzOfType != null)
	    sb.append(", klazz=").append(klazzOfType.getName());
	if (ancestorOfType != null)
	    sb.append(", super=").append(ancestorOfType.toString());
	return sb.append(" }").toString();
    }

}
