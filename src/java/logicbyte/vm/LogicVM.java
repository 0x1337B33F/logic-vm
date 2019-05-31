package logicbyte.vm;

import java.util.List;
import logicbyte.bytecode.LinkerObjectFile;
import logicbyte.loader.DynamicLibraryFile.DynamicLibraryException;
import logicbyte.vm.exception.LinkerNotLoadedException;
import logicbyte.vm.exception.NoSuchFunctionException;
import logicbyte.vm.exception.UnsatisfiedImportException;
import logicbyte.vm.exception.VMException;
import logictype.Log;

/**
 *
 * @author Lochie
 */
public interface LogicVM {

    /**
     * Assign the default logger to use for this virtual machine. Subsequently generated function invokables, operations on the VM instance
     * and other traceable events shall be passed to the logger.
     *
     * @param receiver The logging receiver implementation, or the void logger instance
     */
    public void setLogger(Log receiver);

    /**
     * Load and create a global scope for the linker specified. If the linker imports other declarations, those declarations shall be
     * searched for using the library loader specified and loaded into the current virtual machine, creating as many global scope(s) as
     * required.
     *
     * @param callback The debugger callback
     * @param linker   The linker to load
     *
     * @return The created global closure object
     *
     * @throws InterruptedException                              If the loading operation is interrupted, the interrupt will not be absorbed
     * @throws DynamicLibraryException                           If the dynamic library loader encounters an exception, a
     *                                                           {@link DynamicLibraryException} will be thrown.
     * @throws VMException                                       If the virtual machine cannot initialize the linker specified, or a
     *                                                           required linker, a {@link VMException} will be thrown.
     * @throws logicbyte.vm.exception.UnsatisfiedImportException If the linker, or a dependent linker references an unresolvable symbol, a
     *                                                           {@link UnsatisfiedImportException} will be thrown.
     */
    public VMClosure load(final VMDebugCallback callback, final LinkerObjectFile linker) throws InterruptedException,
	    DynamicLibraryException, VMException, UnsatisfiedImportException;

    /**
     * Get the list of loaded linkers.
     *
     * @param ptr The destination List
     */
    public void loaded(final List<LinkerObjectFile> ptr);

    /**
     * Unload and dispose a global scope for the linker specified. If the linker specified exports declarations that are in use by other
     * linkers, an exception will be thrown.
     *
     * @param linker The linker to unload
     *
     * @throws UnsatisfiedImportException If a dependent linker references symbol(s) in the linker being unloaded, an
     *                                    {@link UnsatisfiedImportException} will be thrown.
     */
    public void unload(final LinkerObjectFile linker) throws UnsatisfiedImportException;

    /**
     * Get the global closure object for the linker, or <code>null</code> if the linker is not loaded.
     *
     * @param linker The linker object
     *
     * @return The existing global closure object, or <code>null</code>
     */
    public VMClosure getClosure(final LinkerObjectFile linker);

    /**
     * Define a native type for this VM instance which has the specified friendly name; the type-name specified shall implicitly extend the
     * VM 'handle' type. The type-name provided shall be used when referencing the Java object in the VM.
     *
     * @param definer        The closure object which defines the type
     * @param handleTypeName The type-name to use to reference the Java type provided
     * @param klazz          The Java class type
     *
     * @return The created type object
     *
     * @throws VMException If the type cannot be defined, a {@link VMException} will be thrown.
     */
    public VMType defineJType(final VMClosure definer, String handleTypeName, Class<?> klazz) throws VMException;

    /**
     * Define a native type for this VM instance which has the specified friendly name; the type-name specified shall extend the previously
     * defined Java type by type-name. The type-name provided shall be used when referencing the Java object in the VM; the Java type
     * provided must be assignable to the super-type provided.
     *
     * @param definer         The closure object which defines the type
     * @param handleTypeName  The type-name to use to reference the Java type provided
     * @param superHandleType The supertype which is an ancestor of the Java type specified by the name
     * @param klazz           The Java class type
     *
     * @return The created type object
     *
     * @throws VMException If the type cannot be defined, a {@link VMException} will be thrown.
     */
    public VMType defineJType(final VMClosure definer, String handleTypeName, VMType superHandleType, Class<?> klazz) throws VMException;

    /**
     * Define a callable native function for this VM instance which is of the {@link BaseVMJFunctionImpl} type; the target function
     * implementation may be an instance method, a static method or an anonymous lambda function (as per generation function in
     * {@link BaseVMJFunctionImpl}. On call to the native function in the VM, the VMJFunction shall be invoked from the thread which the
     * advance call was made.
     *
     * @param funcName The name of the native function, unique
     * @param impl     The VMJFunction implementation
     *
     * @return If the native function was registered with the virtual machine
     */
    public boolean defineJFunc(String funcName, VMJFunction impl);

    /**
     * Define a native class-less type for this VM instance which has the specified friendly name; the type-name specified shall implicitly
     * extend the VM 'handle' type.
     *
     * @param definer  The closure object which defines the type
     * @param typeName The type-name
     *
     * @return The created type object
     *
     * @throws VMException If the type cannot be defined, a {@link VMException} will be thrown.
     */
    public VMType defineType(final VMClosure definer, String typeName) throws VMException;

    /**
     * Define a native class-less type for this VM instance which has the specified friendly name; the type-name specified shall extend the
     * previously defined type-name.
     *
     * @param definer   The closure object which defines the type
     * @param typeName  The type-name
     * @param superType The super-type
     *
     * @return The created type object
     *
     * @throws VMException If the type cannot be defined, a {@link VMException} will be thrown.
     */
    public VMType defineType(final VMClosure definer, String typeName, VMType superType) throws VMException;

    /**
     * Get a type-object for the type-name provided.
     *
     * @param definer  The closure object which defines the type
     * @param typeName The type-name
     *
     * @return The type object, or <code>null</code> if the type does not exist
     */
    public VMType getType(final VMClosure definer, String typeName);

    /**
     * <p>
     * Get the VM type-name for a Class provided.</p>
     *
     * <ul>
     * <li>If <code>null</code>, returns <code>nothing</code>;</li>
     * <li>If one of tbe base primitive types (boolean, integer, double, string), returns the appropriate type-word;</li>
     * <li>If a {@link BaseVMFunctionPointerImpl}, returns <code>code</code>;</li>
     * <li>If a known registered Class type, returns the type-word assigned to the Java class; or</li>
     * <li>returns <code>handle</code> for all unknown Java types.</li>
     * </ul>
     *
     * @param definer The defining closure
     * @param klazz   The class to get the type-name for
     *
     * @return The type name, one of <code>nothing</code>, <code>code</code>, <code>boolean</code>, <code>integer</code>, <code>real</code>,
     *         <code>string</code>, a registered type-name extending <code>handle</code> or <code>handle</code>.
     */
    public VMType getJType(final VMClosure definer, Class<?> klazz);

    /**
     * <p>
     * Determine if a value of the source type can be cast to the destination type, by:</p>
     *
     * <ul>
     * <li>An exact type-match where <code>destType</code> = <code>srcType</code>;</li>
     * <li>A type-widening, where <code>srcType</code> implements <code>destType</code>;</li>
     * <li>An implicit cast from <code>srcType</code> to <code>destType</code>.</li>
     * </ul>
     *
     * @param destType The destination type
     * @param srcType  The source type
     *
     * @return If the value of source type can be cast to the destination type by exact type match, by type widening or implicit casting.
     *
     * @throws VMException
     */
    public boolean canCastToType(final VMType destType, final VMType srcType) throws VMException;

    /**
     * Get a pointer reference to a function defined in the bytecode associated with a referent linker. The returned pointer can be used
     * either in the VM (such as for <code>code</code> function pointer references), or in Java for later invocation with
     * {@link #call(logicbyte.vm.VMDebugCallback, logicbyte.bytecode.LinkerObjectFile, logicbyte.vm.VMFunctionPointer, java.lang.Object[])}.
     *
     * @param linker         The linker
     * @param nameOfFunction The name of the function defined in the bytecode
     *
     * @return A pointer reference to the bytecode function, or null if no such function exists
     *
     * @throws LinkerNotLoadedException If the linker specified is not loaded, a {@link LinkerNotLoadedException} is thrown.
     * @throws NoSuchFunctionException  If the function does not exist, a {@link NoSuchFunctionException} is thrown.
     */
    public VMFunctionPointer getFunction(LinkerObjectFile linker, String nameOfFunction) throws LinkerNotLoadedException, NoSuchFunctionException;

    /**
     * <p>
     * Invokes a function by pointer reference; the returned executor instance is a repeat-callable container which can be used to
     * individually step through each operand in the function until completion. Multiple executor instances for functions can be created,
     * and, multiple executor instances may exist for many functions. Only one executor instance may execute concurrently per-VM; threaded
     * calls to the {@link BaseVMInvokableImpl#advance(logicbyte.vm.VMDebugCallback)} will block if the VM is already executing a
     * function.</p>
     *
     *
     * @param debug   The debugger instance for the function call, which shall be bound to the function for the lifetime of the executor
     *                object
     * @param pointer The function pointer
     *
     * @return A step-based executor reference which can be advanced per-operand, reset and debugged.
     */
    public VMInvokable invoke(VMDebugCallback debug, VMFunctionPointer pointer);

    /**
     * <p>
     * Invokes a function by function reference; the returned executor instance is a repeat-callable container which can be used to
     * individually step through each operand in the function until complete.</p>
     *
     * @see #invoke(logicbyte.vm.VMDebugCallback, logicbyte.vm.VMFunctionPointer)
     * @param debug    The debugger instance for the function call, which shall be bound to the function for the lifetime of the executor
     *                 object.
     * @param function The function reference
     *
     * @return A step-based executor reference which can be advanced per-operand, reset and debugged.
     */
    public VMInvokable invoke(VMDebugCallback debug, VMFunction function);

    /**
     * <p>
     * Obtain the result of an executor instance, or <code>null</code> if the executor has not yet completed.</p>
     *
     * @param inst The executor instance
     *
     * @return The return result, or <code>null</code>
     */
    public Object result(VMInvokable inst);

    /**
     * <p>
     * Obtain the result array of an executor instance as a native <code>Object[]</code> array, or <code>null</code> if the executor has not
     * yet completed.</p>
     *
     * @param inst The executor instance
     *
     * @return The return result array, or <code>null</code>
     */
    public Object[] resultArray(VMInvokable inst);

    /**
     * <p>
     * Reset the executor instance provided, so that a new entry call can be made to the function with different argument(s).</p>
     *
     * @param inst The executor instance
     */
    public void reset(VMInvokable inst);

    /**
     * <p>
     * Determine if the executor instance has yielded.</p>
     *
     * @param inst The executor instance
     *
     * @return If the executor instance has yielded or not
     */
    public boolean yielded(VMInvokable inst);

    /**
     * <p>
     * Obtain the yielded value from the executor instance, or <code>null</code> if the yield did not yield a value.</p>
     *
     * @param inst The executor instance
     *
     * @return The yielded value as a native Java value, or <code>null</code> if the yield did not yield a value
     */
    public Object yield(VMInvokable inst);

}
