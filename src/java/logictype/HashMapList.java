package logictype;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * Implements a Map whose value-type is a varying List of infinite dimension. HashMapList behaves like a Map with the signature
 * <code>HashMap&lt;K, LinkedList&lt;V&gt;&gt;</code>, but instead only stores values as <code>List&lt;V&gt;</code> when required.
 *
 * @param <K> The key type
 * @param <V> The value type
 *
 * @author Lochie
 */
public class HashMapList<K, V> {

    public static class HashMapListEntry< K, V> implements Entry< K, V> {

	final HashMapListIterator<K, V> iterator;

	protected HashMapListEntry(HashMapListIterator< K, V> iterator) {
	    this.iterator = iterator;
	}

	@Override
	public K getKey() {
	    return this.iterator.currentKey;
	}

	@Override
	public V getValue() {
	    return this.iterator.currentValue;
	}

	@Override
	public V setValue(V value) {
	    throw new UnsupportedOperationException("Cannot setValue on HashMapList types");
	}
    }

    /**
     * <p>
     * Implements the Iterator for the Map whose value-type is a varying List of infinite dimension. HashMapListIterator behaves like a Map
     * iterator, except it generates multiple Entry instance(s) for value entries who are of List type; that is, the iterator shall descend
     * directly into each value for the associated key, or the literal (singular) value for the key.</p>
     *
     * <p>
     * For example, the key-value tree: <code> a => { 1, 2, 3 }, b => 0, c => { 4, 5, 6 }</code> would yield the iteration Entry values:
     * <code>[a, 1], [a, 2], [a, 3], [b, 0], [c, 4], [c, 5], [c, 6]</code>.</p>
     *
     * <p>
     * The key-values are iterated in key-set order, as per the Map definition. The value-values are iterated in value-order, as per the
     * List definition. The interpolation of the two yields an Iterator which shall iterate over keys in key-set order, followed by all (one
     * or more entries) in value-set order.</p>
     *
     * @param <K> The key type
     * @param <V> The value type
     */
    public static class HashMapListIterator<K, V> implements Iterator<Map.Entry< K, V>> {

	final HashMapList<K, V> iterable;
	final HashMapListEntry<K, V> entrySingleton;

	/**
	 * The outer key-set K iterator
	 */
	volatile Iterator<K> keyIterator;
	/**
	 * The current iterable key K
	 */
	volatile K currentKey;

	/**
	 * The inner value-set V iterator
	 */
	volatile Iterator<V> valueIterator;
	/**
	 * The current value V
	 */
	volatile V currentValue;

	protected HashMapListIterator(HashMapList<K, V> iterable) {
	    this.iterable = iterable;
	    this.entrySingleton = new HashMapListEntry<>(this);
	    this.keyIterator = iterable.backingMap.keySet().iterator();
	}

	@Override
	public boolean hasNext() {
	    if (this.valueIterator != null)
		return this.valueIterator.hasNext() || this.keyIterator.hasNext();
	    return this.keyIterator.hasNext();
	}

	@Override
	public Entry<K, V> next() {
	    // Dispose second-dimension iterator if no more elements {e} in set:
	    if (this.valueIterator != null && !this.valueIterator.hasNext())
		this.valueIterator = null;

	    // If no second-dimension iterator of element-set {e}, then:
	    if (this.valueIterator == null) {
		// advance to the next key in the key-set {k}:
		this.currentKey = this.keyIterator.next();
		// peek what the right-hand-side value is, either literal V or List<V>:
		Object right = this.iterable.backingMap.get(currentKey);
		if (right instanceof List) // it's a list:
		    // expand the second-dimension iterator now:
		    this.valueIterator = ((List) right).iterator();
		else {
		    // return the Entry literal for the pair [ currentKey, right ]
		    this.currentValue = (V) right;
		    return this.entrySingleton;
		}
	    }

	    // If a second-dimension iterator for set {e} exists:
	    if (this.valueIterator != null) {
		// Next e in set {e}:
		this.currentValue = (V) this.valueIterator.next();
		// return the Entry literal for the pair [ currentKey, next ]
		return this.entrySingleton;
	    }

	    return null;
	}

	@Override
	public void remove() {
	    throw new UnsupportedOperationException("Cannot remove on HashMapList types");
	}

	@Override
	public void forEachRemaining(Consumer<? super Entry<K, V>> action) {
	    while (hasNext())
		action.accept(next());
	}

    }

    private final HashMap<K, Object> backingMap;

    public HashMapList() {
	this.backingMap = new HashMap<>();
    }

    /**
     * @see Map#size()
     * @return The map size
     */
    public int size() {
	return backingMap.size();
    }

    /**
     * @see Map#isEmpty()
     * @return If the map is empty or not
     */
    public boolean isEmpty() {
	return backingMap.isEmpty();
    }

    /**
     * @see Map#clear()
     */
    public void clear() {
	backingMap.clear();
    }

    /**
     * @see Map#containsKey(java.lang.Object)
     * @param theKeyValue The key value
     *
     * @return If the map contains the key value
     */
    public boolean containsKey(K theKeyValue) {
	return backingMap.containsKey(theKeyValue);
    }

    /**
     * Add an item to the Map. If this is the first item for the key in the Map, then the value is stored literally; otherwise, the existing
     * entry is converted to a List containing the previous value-element and the new value element, in order.
     *
     * @param theKeyValue   The key value
     * @param theValueValue The value value
     */
    public void add(K theKeyValue, V theValueValue) {
	if (backingMap.containsKey(theKeyValue)) {
	    Object right = backingMap.get(theKeyValue);
	    List<V> list;
	    if (right instanceof List)
		list = (List<V>) right;
	    else {
		backingMap.put(theKeyValue, list = new LinkedList<>());
		list.add((V) right);
	    }
	    list.add(theValueValue);
	} else
	    backingMap.put(theKeyValue, theValueValue);
    }

    /**
     * Get a value from the Map.
     *
     * @param theKeyValue   The key value
     * @param theIndexValue The index of the value value
     *
     * @return The value value associated with the key at the index provided
     */
    public V get(K theKeyValue, int theIndexValue) {
	Object right = backingMap.get(theKeyValue);
	if (right instanceof List)
	    return ((List<V>) right).get(theIndexValue);
	else if (right != null) {
	    if (theIndexValue != 0)
		throw new IndexOutOfBoundsException();
	    return (V) right;
	} else
	    return null;
    }

    /**
     * Remove a value from the Map. If the operation results in the value List containing only one element, then the value List will be
     * flattened to the single remaining value element.
     *
     * @param theKeyValue   The key value
     * @param theValueValue The value value
     *
     * @return The value which was removed, or <code>null</code> if no modification occurred
     */
    public V remove(K theKeyValue, V theValueValue) {
	Object right = backingMap.get(theKeyValue);
	if (right instanceof List) {
	    List<V> list = (List<V>) right;
	    if (list.remove(theValueValue)) {
		if (list.size() <= 1)
		    backingMap.put(theKeyValue, list.get(0));
		return theValueValue;
	    } else
		return null;
	} else if (right != null)
	    return (V) backingMap.remove(theKeyValue);
	else
	    return null;
    }

    public V[] remove(K theKeyValue) {
	if (backingMap.containsKey(theKeyValue)) {
	    Object right = backingMap.remove(theKeyValue);
	    if (right instanceof List)
		return (V[]) ((List) right).toArray();
	    else
		return (V[]) new Object[] {(V) right};
	} else
	    return null;
    }

    /**
     * Return the count of value(s) associated with a key in the Map. If the value is a List, returns the size of the list (number of
     * elements associated with the key); otherwise, returns <code>1</code> if a literal value or <code>0</code> if no such key exists.
     *
     * @param theKeyValue The key value
     *
     * @return The number of elements associated with the key
     */
    public int count(K theKeyValue) {
	Object right = backingMap.get(theKeyValue);
	if (right instanceof List)
	    return ((List<V>) right).size();
	else if (right != null)
	    return 1;
	else
	    return 0;
    }

    public Iterator<Entry<K, V>> iterator() {
	return new HashMapListIterator<>(this);
    }

}
