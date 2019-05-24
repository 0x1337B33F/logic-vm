package logicbyte.vm;

import java.util.LinkedList;

/**
 *
 * @author Lochie
 */
public interface VMTypeIndex {

    public VMType get(final VMClosure definer, final String name);

    public void put(final VMClosure definer, final VMType impl);

    public LinkedList<VMType> leaves(final VMClosure definer);

    public void unload(final VMClosure definer);

}
