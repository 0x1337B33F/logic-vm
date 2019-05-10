package logicbyte.vm.runtime.func;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.runtime.BaseVMJFunctionImpl;
import logicbyte.vm.VMJFunctionLambda;
import logicbyte.vm.exception.ThrowObjectException;

/**
 *
 * @author Lochie
 */
public class ListFuncs {

    public static Iterator<?> newGenericIterator(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException {
	if (args.length != 1)
	    throw new ThrowObjectException("list not provided");
	if (!(args[0] instanceof List<?>))
	    throw new ThrowObjectException("argument 1 not list");
	return ((List<?>) args[0]).iterator();
    }

    public static int getListSize(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException {
	if (args.length != 1)
	    throw new ThrowObjectException("list not provided");
	if (!(args[0] instanceof List<?>))
	    throw new ThrowObjectException("argument 1 not list");
	return ((List<?>) args[0]).size();
    }

    static class ListAllocatorInternalTyped<T> implements VMJFunctionLambda {

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] arguments) throws ThrowObjectException, Exception {
	    return new LinkedList<T>();
	}
    };

    /**
     * Extension of the JFunctionLambda interface as abstract class whom checks the first argument is a list type, then passes the list and
     * arguments to the tailcall method.
     *
     * @param <T> The class-type of the list.
     * @param <R> The return type of the tailcall method.
     */
    abstract static class ListFuncInternalTyped<T, R> implements VMJFunctionLambda {

	/**
	 * Class of the generic T
	 */
	protected final Class<T> theTypeOfT;
	/**
	 * Class of the generic R
	 */
	protected final Class<R> theTypeOfR;

	ListFuncInternalTyped(Class<T> theTypeOfT, Class<R> theTypeOfR) {
	    this.theTypeOfT = theTypeOfT;
	    this.theTypeOfR = theTypeOfR;
	}

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException, Exception {
	    if (args.length == 0)
		throw new ThrowObjectException("list not provided");
	    if (!(args[0] instanceof List<?>))
		throw new ThrowObjectException("argument 1 not list");
	    return tailcall(((List<T>) args[0]), args);
	}

	/**
	 * Perform JFunctionLambda call after casting the passed first-argument to the List&lt;T&gt; type.
	 *
	 * @param list The cast List&lt;T&gt; value
	 * @param args The original argument list
	 *
	 * @return A value of type <code>R</code>
	 *
	 * @throws ThrowObjectException
	 */
	public abstract R tailcall(List<T> list, Object[] args) throws ThrowObjectException;
    };

    /**
     * Implementation of the lambda extension which gets list values of type T and returns them as if R implements T.
     *
     * @param <T> The type of the list, the return type of the method.
     */
    static class ListFuncInternalGetter<T> extends ListFuncInternalTyped<T, T> {

	public ListFuncInternalGetter(Class<T> theTypeOfT) {
	    super(theTypeOfT, theTypeOfT);
	}

	@Override
	public T tailcall(List<T> list, Object[] args) throws ThrowObjectException {
	    if (args.length != 2)
		throw new ThrowObjectException("index not provided");
	    if (!(args[1] instanceof Integer)) // FIXME: will this be the boxed type ?
		throw new ThrowObjectException("argument 2 not integer");
	    int idx = (Integer) args[1];
	    final Object theValueObj = list.get(idx);
	    if (!(theTypeOfT.isAssignableFrom(theValueObj.getClass())))
		throw new ThrowObjectException(String.format("cannot convert type %s to type %s", theValueObj.getClass().getName(),
			theTypeOfT.getClass().getName()));
	    return (T) theValueObj;
	}
    };

    /**
     * Implementation of the lambda extension which puts list values of type T onto the list and returns Boolean.
     *
     * @param <T> The type of the list.
     */
    static class ListFuncInternalPutter<T> extends ListFuncInternalTyped<T, Boolean> {

	public ListFuncInternalPutter(Class<T> theTypeOfT) {
	    super(theTypeOfT, Boolean.class);
	}

	@Override
	public Boolean tailcall(List<T> list, Object[] args) throws ThrowObjectException {
	    if (args.length != 2)
		throw new ThrowObjectException("value not provided");
	    if (!theTypeOfT.isAssignableFrom(args[1].getClass()))
		throw new ThrowObjectException(String.format("cannot convert type %s to type %s", args[1].getClass().getName(),
			theTypeOfT.getName()));
	    return list.add(theTypeOfT.cast(args[1]));
	}
    };

    /**
     * Implementation of the lambda extension which sets list values of type T in the list and returns Boolean.
     *
     * @param <T> The type of the list.
     */
    static class ListFuncInternalSetter<T> extends ListFuncInternalTyped<T, Boolean> {

	public ListFuncInternalSetter(Class<T> theTypeOfT) {
	    super(theTypeOfT, Boolean.class);
	}

	@Override
	public Boolean tailcall(List<T> list, Object[] args) throws ThrowObjectException {
	    if (args.length != 3)
		throw new ThrowObjectException("value not provided");
	    if (!(args[1] instanceof Integer))
		throw new ThrowObjectException("argument 2 not integer");
	    if (!theTypeOfT.isAssignableFrom(args[2].getClass()))
		throw new ThrowObjectException(String.format("cannot convert type %s to type %s", args[2].getClass().getName(),
			theTypeOfT.getName()));
	    list.set((Integer) args[1], theTypeOfT.cast(args[2]));
	    return true;
	}

    }

    public static boolean genericIteratorHasNext(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException {
	if (args.length != 1)
	    throw new ThrowObjectException("list not provided");
	if (!(args[0] instanceof Iterator<?>))
	    throw new ThrowObjectException("argument 1 not iterator");
	return ((Iterator<?>) args[0]).hasNext();
    }

    static class IteratorFuncInternalNext<T> implements VMJFunctionLambda {

	@Override
	public Object call(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] args) throws ThrowObjectException, Exception {
	    if (args.length == 0)
		throw new ThrowObjectException("list not provided");
	    if (!(args[0] instanceof Iterator<?>))
		throw new ThrowObjectException("argument 1 not iterator");
	    Iterator<T> iteratorObj = (Iterator<T>) args[0];
	    return iteratorObj.next();
	}
    };

    public static void apply(LogicVM inst) throws NoSuchMethodException {

	inst.defineJFunc("List_NewB", BaseVMJFunctionImpl.generateFunctionLambda(new ListAllocatorInternalTyped<Boolean>()));
	inst.defineJFunc("List_NewI", BaseVMJFunctionImpl.generateFunctionLambda(new ListAllocatorInternalTyped<Integer>()));
	inst.defineJFunc("List_NewR", BaseVMJFunctionImpl.generateFunctionLambda(new ListAllocatorInternalTyped<Double>()));
	inst.defineJFunc("List_NewS", BaseVMJFunctionImpl.generateFunctionLambda(new ListAllocatorInternalTyped<String>()));
	inst.defineJFunc("List_NewH", BaseVMJFunctionImpl.generateFunctionLambda(new ListAllocatorInternalTyped<>()));

	inst.defineJFunc("List_GetB", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalGetter<>(Boolean.class)));
	inst.defineJFunc("List_GetI", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalGetter<>(Integer.class)));
	inst.defineJFunc("List_GetR", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalGetter<>(Double.class)));
	inst.defineJFunc("List_GetS", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalGetter<>(String.class)));
	inst.defineJFunc("List_GetH", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalGetter<>(Object.class)));

	inst.defineJFunc("List_PutB", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalPutter<>(Boolean.class)));
	inst.defineJFunc("List_PutI", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalPutter<>(Integer.class)));
	inst.defineJFunc("List_PutR", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalPutter<>(Double.class)));
	inst.defineJFunc("List_PutS", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalPutter<>(String.class)));
	inst.defineJFunc("List_PutH", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalPutter<>(Object.class)));

	inst.defineJFunc("List_SetB", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalSetter<>(Boolean.class)));
	inst.defineJFunc("List_SetI", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalSetter<>(Integer.class)));
	inst.defineJFunc("List_SetR", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalSetter<>(Double.class)));
	inst.defineJFunc("List_SetS", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalSetter<>(String.class)));
	inst.defineJFunc("List_SetH", BaseVMJFunctionImpl.generateFunctionLambda(new ListFuncInternalSetter<>(Object.class)));

	inst.defineJFunc("List_GetIterator", BaseVMJFunctionImpl.generateFunctionStatic(ListFuncs.class, "newGenericIterator"));
	inst.defineJFunc("List_Size", BaseVMJFunctionImpl.generateFunctionStatic(ListFuncs.class, "getListSize"));

	inst.defineJFunc("Iterator_NextB", BaseVMJFunctionImpl.generateFunctionLambda(new IteratorFuncInternalNext<Boolean>()));
	inst.defineJFunc("Iterator_NextI", BaseVMJFunctionImpl.generateFunctionLambda(new IteratorFuncInternalNext<Integer>()));
	inst.defineJFunc("Iterator_NextR", BaseVMJFunctionImpl.generateFunctionLambda(new IteratorFuncInternalNext<Double>()));
	inst.defineJFunc("Iterator_NextS", BaseVMJFunctionImpl.generateFunctionLambda(new IteratorFuncInternalNext<String>()));
	inst.defineJFunc("Iterator_NextH", BaseVMJFunctionImpl.generateFunctionLambda(new IteratorFuncInternalNext<>()));

	inst.defineJFunc("Iterator_HasNext", BaseVMJFunctionImpl.generateFunctionStatic(ListFuncs.class, "genericIteratorHasNext"));
    }

}
