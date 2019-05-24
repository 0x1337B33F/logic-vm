package logicbyte.vm.runtime;

import java.util.HashMap;
import java.util.LinkedList;
import logicbyte.vm.VMClosure;
import logicbyte.vm.VMType;
import logicbyte.vm.VMTypeIndex;

/**
 *
 * @author Lochie
 */
public class BaseVMTypeIndex implements VMTypeIndex {

    private final HashMap< VMClosure, HashMap< String, VMType>> map;

    private final HashMap< VMClosure, LinkedList< VMType>> leafMap;

    public BaseVMTypeIndex() {
	this.map = new HashMap<>();
	this.leafMap = new HashMap<>();
    }

    @Override
    public VMType get(VMClosure definer, String name) {
	HashMap<String, VMType> subMap = null;
	synchronized ( this.map ) {
	    subMap = this.map.get(definer);
	}
	if (subMap == null)
	    return null;
	synchronized ( subMap ) {
	    return subMap.get(name);
	}
    }

    @Override
    public void put(VMClosure definer, VMType impl) {
	HashMap<String, VMType> subMap = null;
	synchronized ( this.map ) {
	    subMap = this.map.get(definer);
	}
	if (subMap == null)
	    synchronized ( this.map ) {
		this.map.put(definer, subMap = new HashMap<>());
	    }
	synchronized ( subMap ) {
	    subMap.put(impl.name(), impl);
	}

	if (impl.supertype() == null) {
	    LinkedList< VMType> leafList = null;
	    synchronized ( this.leafMap ) {
		leafList = this.leafMap.get(definer);
	    }
	    if (leafList == null)
		synchronized ( this.leafMap ) {
		    this.leafMap.put(definer, leafList = new LinkedList<>());
		}
	    synchronized ( leafList ) {
		leafList.add(impl);
	    }
	}
    }

    @Override
    public LinkedList<VMType> leaves(final VMClosure definer) {
	synchronized ( this.leafMap ) {
	    return this.leafMap.get(definer);
	}
    }

    @Override
    public void unload(VMClosure definer) {
	synchronized ( this.map ) {
	    this.map.remove(definer);
	}
    }

}
