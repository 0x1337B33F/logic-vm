package logictype;

import java.util.HashMap;

/**
 * Bi-directional HashMap implementation. Includes the <code>reverse</code> method, which returns the reverse of the HashMap's key-value
 * pairs (value-key) type.
 *
 * @param <K>
 * @param <V>
 *
 * @author Lochie
 */
public class BiDiMap<K, V> extends HashMap<K, V> {

    final HashMap<V, K> reverseMap;

    public BiDiMap() {
	super();
	this.reverseMap = new HashMap<V, K>();
    }

    @Override
    public V put(K key, V value) {
	V oldV = super.put(key, value);
	reverseMap.put(value, key);
	return oldV;
    }

    @Override
    public V remove(Object key) {
	V oldV = super.remove(key);
	reverseMap.remove(oldV);
	return oldV;
    }

    @Override
    public V replace(K key, V value) {
	V oldV = super.replace(key, value);
	if (oldV != null)
	    reverseMap.put(value, key);
	return oldV;
    }

    public HashMap<V, K> reverse() {
	return reverseMap;
    }
}
