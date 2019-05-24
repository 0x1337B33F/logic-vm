package logicbyte.vm.runtime;

import logicbyte.vm.VMFunction;
import logicbyte.vm.VMInvokableAccess;
import logicbyte.vm.VMType;
import logicbyte.vm.runtime.BaseVMInvokableImpl.Closure;
import logicbyte.vm.runtime.BaseVMInvokableImpl.FunctionCallContext;
import logictype.Pair;

/**
 *
 * @author Lochie
 */
public class BaseVMInvokableAccessImpl implements VMInvokableAccess {

    final BaseVMInvokableImpl outer;

    public BaseVMInvokableAccessImpl(BaseVMInvokableImpl outer) {
	this.outer = outer;
    }

    private Pair<VMFunction, FunctionCallContext> context(int stackSlot) throws IndexOutOfBoundsException {
	try {
	    return this.outer.callStack.get(stackSlot);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new IndexOutOfBoundsException(e.getMessage());
	}
    }

    private Closure closure(int stackSlot, int closureSlot) {
	try {
	    FunctionCallContext context = context(stackSlot).b();
	    return context.functionClosureStack.get(closureSlot);
	} catch (ArrayIndexOutOfBoundsException e) {
	    throw new IndexOutOfBoundsException(e.getMessage());
	}
    }

    @Override
    public VMFunction getEntryFunction() {
	return this.outer.entryFunc.dereference();
    }

    @Override
    public int getCallStackSize() {
	return this.outer.callStack.size();
    }

    @Override
    public VMFunction getFunctionOnCallStack(int stackSlot) {
	return this.context(stackSlot).a();
    }

    @Override
    public int getFunctionProgramCounter(int stackSlot) {
	return this.context(stackSlot).b().programCounterValue;
    }

    @Override
    public boolean getFunctionComparisonRegister(int stackSlot) {
	return this.context(stackSlot).b().comparisonRegisterValue;
    }

    @Override
    public Object getFunctionCaughtExceptionObj(int stackSlot) {
	return this.context(stackSlot).b().functionCaughtExceptionObj;
    }

    @Override
    public boolean getFunctionDesiresCallResult(int stackSlot) {
	return this.context(stackSlot).b().desiresCallResult;
    }

    @Override
    public int getFunctionClosureCount(int stackSlot) {
	return this.context(stackSlot).b().functionClosureStack.size();
    }

    @Override
    public int getFunctionClosureLocalsStart(int stackSlot, int closureSlot) {
	return this.closure(stackSlot, closureSlot).startOfClosureLocals;
    }

    @Override
    public int getFunctionClosureLocalsCount(int stackSlot, int closureSlot) {
	return this.closure(stackSlot, closureSlot).numberOfClosureLocals;
    }

    @Override
    public VMType getFunctionClosureLocalsType(int stackSlot, int closureSlot, int localNumber) {
	return this.closure(stackSlot, closureSlot).typesOfClosureLocals.get(localNumber);
    }

    @Override
    public boolean getFunctionClosureLocalsArray(int stackSlot, int closureSlot, int localNumber) {
	return this.closure(stackSlot, closureSlot).arrayTypeOfClosureLocal.get(localNumber);
    }

    @Override
    public int getFunctionClosureLocalsArraySize(int stackSlot, int closureSlot, int localNumber) {
	Object valueOfLocal = this.closure(stackSlot, closureSlot).valuesOfClosureLocals.get(localNumber);
	if (!(valueOfLocal instanceof BaseVMArrayImpl))
	    throw new ClassCastException("local is not array type");
	return ((BaseVMArrayImpl) valueOfLocal).sizeOfArray;
    }

    @Override
    public Object getFunctionClosureLocalsValue(int stackSlot, int closureSlot, int localNumber) {
	return this.closure(stackSlot, closureSlot).valuesOfClosureLocals.get(localNumber);
    }

    @Override
    public Object getFunctionClosureLocalsArrayValue(int stackSlot, int closureSlot, int localNumber, int arraySlot) {
	Object valueOfLocal = this.closure(stackSlot, closureSlot).valuesOfClosureLocals.get(localNumber);
	if (!(valueOfLocal instanceof BaseVMArrayImpl))
	    throw new ClassCastException("local is not array type");
	return ((BaseVMArrayImpl) valueOfLocal).arrayValues[arraySlot];
    }

    @Override
    public int getFunctionStackCount(int stackSlot) {
	return this.context(stackSlot).b().functionStackStack.size();
    }

    @Override
    public Object getFunctionStackValue(int stackSlot, int funcStackSlot) {
	return this.context(stackSlot).b().functionStackStack.get(funcStackSlot);
    }

    @Override
    public int getFunctionTryCatchStackCount(int stackSlot) {
	return this.context(stackSlot).b().functionTryCatchStack.size();
    }

    @Override
    public int getFunctionTryCatchStackJumpDest(int stackSlot, int tryCatchStackSlot) {
	return this.context(stackSlot).b().functionTryCatchStack.get(tryCatchStackSlot).a();
    }

    @Override
    public int getFunctionTryCatchStackClosureDepth(int stackSlot, int tryCatchStackSlot) throws IndexOutOfBoundsException {
	return this.context(stackSlot).b().functionTryCatchStack.get(tryCatchStackSlot).b();
    }

}
