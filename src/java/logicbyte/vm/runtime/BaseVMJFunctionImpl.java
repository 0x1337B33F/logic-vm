package logicbyte.vm.runtime;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import logicbyte.vm.LogicVM;
import logicbyte.vm.VMDebugCallback;
import logicbyte.vm.VMInvokable;
import logicbyte.vm.VMJFunction;
import logicbyte.vm.VMJFunctionLambda;
import logicbyte.vm.exception.ThrowObjectException;

/**
 *
 * <p>
 * Implements a function wrapper which can be used to invoke a Java instance method, static method or anonymous lambda function from a
 * native function call within the VM instance.</p>
 *
 * @author Lochie
 */
public class BaseVMJFunctionImpl implements VMJFunction {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface VMJFunctionLabel {

	public String name();
    }

    private static final Class<?>[] VMJFUNC_SIG = new Class<?>[] {LogicVM.class, VMInvokable.class, VMDebugCallback.class, Object[].class};

    public static Map<String, BaseVMJFunctionImpl> generateFunctionClassStatic(Class<?> klazz) throws NoSuchMethodException, SecurityException {
	HashMap< String, BaseVMJFunctionImpl> result = new HashMap<>();
	Method[] methods = klazz.getMethods();
	for (Method method : methods) {
	    if ((method.getModifiers() & Modifier.STATIC) == 0)
		continue;
	    Annotation[] annotations = method.getAnnotations();
	    if (annotations == null || annotations.length == 0)
		continue;

	    VMJFunctionLabel label = null;
	    for (int i = 0, j = annotations.length; i < j; i++)
		if (annotations[i].annotationType().equals(VMJFunctionLabel.class))
		    label = (VMJFunctionLabel) annotations[i];

	    if (label == null)
		continue;
	    result.put(label.name(), generateFunctionStatic(klazz, method.getName()));
	}
	return result;
    }

    public static Map<String, BaseVMJFunctionImpl> generateFunctionClassInstance(Class<?> klazz, Object invokeObj) throws NoSuchMethodException, SecurityException {
	HashMap< String, BaseVMJFunctionImpl> result = new HashMap<>();
	Method[] methods = klazz.getMethods();
	for (Method method : methods) {
	    if ((method.getModifiers() & Modifier.STATIC) != 0)
		continue;
	    Annotation[] annotations = method.getAnnotations();
	    if (annotations == null || annotations.length == 0)
		continue;

	    VMJFunctionLabel label = null;
	    for (int i = 0, j = annotations.length; i < j; i++)
		if (annotations[i].annotationType().equals(VMJFunctionLabel.class))
		    label = (VMJFunctionLabel) annotations[i];

	    if (label == null)
		continue;
	    result.put(label.name(), generateFunctionInstance(klazz, invokeObj, method.getName()));
	}
	return result;
    }

    /**
     * Generate a VMJFunction from a Class static method; the Class and the method name must be provided.
     *
     * @param klazz      The Class which the static method is defined in
     * @param methodName The name of the static method in the Class
     *
     * @return The VMJFunction wrapper instance
     *
     * @throws NoSuchMethodException If the method does not exist, or does not match the signature expected, a {@link NoSuchMethodException}
     *                               will be thrown.
     * @throws SecurityException     If the current SecurityManager does not allow for reflection, a {@link SecurityException} will be
     *                               thrown.
     */
    public static BaseVMJFunctionImpl generateFunctionStatic(Class<?> klazz, String methodName) throws NoSuchMethodException, SecurityException {
	return new BaseVMJFunctionImpl(null, klazz.getMethod(methodName, VMJFUNC_SIG));
    }

    /**
     * Generate a VMJFunction from a Class instance method; the Class, the instance object of the Class and the method name must be
     * provided. Note that instance method VMJFunction wrappers maintain a strong reference to the invocation target object.
     *
     * @param klazz      The Class which the instance method is defined in
     * @param invokeObj  An instance of the Class which shall be used as the target for invocation
     * @param methodName The name of the instance method in the Class
     *
     * @return The VMJFunction wrapper instance
     *
     * @throws NoSuchMethodException If the method does not exist, or does not match the signature expected, a {@link NoSuchMethodException}
     *                               will be thrown.
     * @throws SecurityException     If the current SecurityManager does not allow for reflection, a {@link SecurityException} will be
     *                               thrown.
     */
    public static BaseVMJFunctionImpl generateFunctionInstance(Class<?> klazz, Object invokeObj, String methodName) throws NoSuchMethodException, SecurityException {
	return new BaseVMJFunctionImpl(invokeObj, klazz.getMethod(methodName, VMJFUNC_SIG));
    }

    /**
     * Generate a VMJFunction from an anonymous lambda implementation (the {@link VMJFunctionLambdaInterface}). The provided Lambda shall be
     * converted directly into a VMJFunction, invoked upon itself.
     *
     * @param lambda The lambda function implementation
     *
     * @return The VMJFunction wrapper for the lambda instance
     *
     * @throws NoSuchMethodException If the method does not exist, or does not match the signature expected, a {@link NoSuchMethodException}
     *                               will be thrown.
     * @throws SecurityException     If the current SecurityManager does not allow for reflection, a {@link SecurityException} will be
     *                               thrown.
     */
    public static BaseVMJFunctionImpl generateFunctionLambda(VMJFunctionLambda lambda) throws NoSuchMethodException, SecurityException {
	return new BaseVMJFunctionImpl(lambda, lambda.getClass().getMethod("call", VMJFUNC_SIG));
    }

    /**
     * The invocation target
     */
    final Object invokeObj;

    /**
     * The invocation reflection method object
     */
    final Method invokeMethod;

    /**
     * Default constructor.
     *
     * @param invokeObj    The invocation target value
     * @param invokeMethod The reflection method object
     */
    protected BaseVMJFunctionImpl(Object invokeObj, Method invokeMethod) {
	this.invokeObj = invokeObj;
	this.invokeMethod = invokeMethod;
	this.invokeMethod.setAccessible(true);
    }

    @Override
    public Object invoke(LogicVM vm, VMInvokable executor, VMDebugCallback debug, Object[] arguments) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ThrowObjectException {
	try {
	    return this.invokeMethod.invoke(this.invokeObj, new Object[] {vm, executor, debug, arguments});
	} catch (InvocationTargetException ite) {
	    /* Unbox InvocationTargetException to true exception: */
	    if (ite.getCause() instanceof ThrowObjectException)
		throw (ThrowObjectException) ite.getCause();
	    throw ite;
	}
    }

}
