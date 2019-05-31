package logicbyte.vm;

import java.util.LinkedList;

/**
 * <p>
 * Contract interface for classes which implement global type indices.</p>
 *
 * @author Lochie
 */
public interface VMTypeIndex {

    /**
     * <p>
     * Get the definition of a type, by name, as known by a defining closure. Specifically, this obtains the type-reference for the type t
     * as viewed by the closure.</p>
     *
     * @param definer The closure to find the type within
     * @param name    The name of the type
     *
     * @return The instance of the type-reference which is known by the closure, or <code>null</code> if the closure does not have a type
     *         with the name registered
     */
    public VMType get(final VMClosure definer, final String name);

    /**
     * <p>
     * Assign a type-reference to a closure. Specifically, this sets the type-reference for the type t as viewed by the closure.</p>
     *
     * @param definer The closure to register the type
     * @param impl    The type instance
     */
    public void put(final VMClosure definer, final VMType impl);

    /**
     * <p>
     * Obtain the list of all type-references which are leaf types; that is, the set of all types which are not ancestors of other type(s).
     * </p>
     *
     * @param definer The closure to find the leaf types within
     *
     * @return The list of all leaf types within the closure, or the empty list
     */
    public LinkedList<VMType> leaves(final VMClosure definer);

    /**
     * <p>
     * Called when a closure is unloaded.</p>
     *
     * @param definer The closure to unload
     */
    public void unload(final VMClosure definer);

}
