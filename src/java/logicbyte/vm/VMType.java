package logicbyte.vm;

/**
 * <p>
 * Contract interface for classes which implement virtual machine type singletons.</p>
 *
 * @author Lochie
 */
public interface VMType {

    /**
     * @return The formal name of the type.
     */
    public String name();

    /**
     * @return The closure object which defined the type.
     */
    public VMClosure closure();

    /**
     * @return The supertype of the type, or <code>null</code> if the supertype is the virtual machine <code>handle</code> type.
     */
    public VMType supertype();

    /**
     * Get the Class which represents this type in the VM. In the case of first-degree abstractions of the <code>handle</code> type, the
     * Class shall be some class that <code>extends Object</code>. Otherwise, the Class shall be some class that extends the supertype's
     * class (for interfaces, those interfaces which extend the superinterface; or for classes, those classes which extend or implement the
     * superclass). If the type is a name-only abstraction (ie, it does not specifically signify a narrowing of the <code>Object</code> or
     * ancestor type), then the class will be <code>null</code>. Indication of <code>null</code> does not mean that a supertype does not
     * narrow from the <code>Object</code> type; it merely indicates that the current type is a synonym of the ancestor type, or
     * <code>Object</code> if no ancestor type is defined.
     *
     * @return The Java type (class or interface) that narrows the type.
     */
    public Class<?> jklazz();

}
