package net.coderodde.util;

import com.sun.xml.internal.ws.server.UnsupportedMediaException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * This class implements a bidirectional hash map mapping keys to values and 
 * values to keys.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Dec 23, 2017)
 * @param <K> the key type.
 * @param <V> the value type.
 */
public final class BidirectionalHashMap<K, V> extends AbstractMap<K, V> {

    /**
     * This static inner class store all the information for representing a 
     * mapping. Also, it caches the hash codes of both the keys and values in
     * order to avoid recomputing those codes.
     * 
     * @param <K> the key type.
     * @param <V> the value type.
     */
    private static final class Mapping<K, V> implements Map.Entry<K, V> {
        
        /**
         * The key.
         */
        K key;
        
        /**
         * The value.
         */
        V value;
        
        /**
         * The hash code of the key.
         */
        int keyHashCode;
        
        /**
         * The hash code of the value.
         */
        int valueHashCode;
        
        /**
         * Constructs a new mapping setting the key and the value along their
         * hash codes.
         * 
         * @param key   the key to set.
         * @param value the value to set.
         */
        Mapping(K key, V value) {
            this.key = key;
            this.value = value;
            this.keyHashCode = Objects.hashCode(key);
            this.valueHashCode = Objects.hashCode(value);
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }
        
        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("");
        }
        
        public String toString() {
            return "[" + Objects.toString(key) 
                       + " -> " 
                       + Objects.toString(value) 
                       + "]";
        }
    }
    
    /**
     * This static inner class implements a collision chain node for keys.
     * 
     * @param <K> the key type.
     * @param <V> the value type.
     */
    private static final class KeyNode<K, V> {
        
        /**
         * Points to the predecessor node in the collision chain.
         */
        KeyNode<K, V> prev;
        
        /**
         * Points to the successor node in the collision chain.
         */
        KeyNode<K, V> next;
        
        /**
         * The actual mapping.
         */
        Mapping<K, V> mapping;
        
        /**
         * Points to the key that was added to this map immediately <bbefore</b>
         * this node.
         */
        KeyNode<K, V> up;
        
        /**
         * Points to the key that was added to this map immediately 
         * <b>after</b> this node.
         */
        KeyNode<K, V> down;
        
        KeyNode(Mapping<K, V> mapping) {
            this.mapping = mapping;
        }
    }
    
    /**
     * This static inner class implements a collision chain node for the values.
     * 
     * @param <K> the key type.
     * @param <V> the value type.
     */
    private static final class ValueNode<K, V> {
        
        /**
         * Points to the predecessor node in the collision chain.
         */
        ValueNode<K, V> prev;
        
        /**
         * Points to the successor node in the collision chain.
         */
        ValueNode<K, V> next;
        
        /**
         * The actual mapping.
         */
        Mapping<K, V> mapping;
        
        /**
         * Points to the value that was added to this map immediately 
         * <b>before</b> this node.
         */
        ValueNode<K, V> up;
        
        /**
         * Points to the value that was added to this map immediately
         * <b>after</b> this node.
         */
        ValueNode<K, V> down;
        
        ValueNode(Mapping<K, V> mapping) {
            this.mapping = mapping;
        }
    }
    
    /**
     * The default capacity. Keeping the capacity as powers of two allows us 
     * using bit masking for computing the modulo.
     */
    private static final int DEFAULT_CAPACITY = 8;
    
    /**
     * The minimum capacity of both the tables.
     */
    private static final int MINIMUM_CAPACITY = 8;
    
    /**
     * The default load factor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 1.0f;
    
    /**
     * The forward hash table mapping keys to the mappings.
     */
    private KeyNode<K, V>[] keyNodes = new KeyNode[DEFAULT_CAPACITY];
    
    /**
     * The backward hash table mapping values to the mappings.
     */
    private ValueNode<K, V>[] valueNodes = new ValueNode[DEFAULT_CAPACITY];
    
    /**
     * Points to the oldest key node.
     */
    private KeyNode<K, V> keyIterationHead;
    
    /**
     * Points to the newest key node.
     */
    private KeyNode<K, V> keyIterationTail;
    
    /**
     * Points to the oldest value node.
     */
    private ValueNode<K, V> valueIterationHead;
    
    /**
     * Points to the newest value node.
     */
    private ValueNode<K, V> valueIterationTail;
    
    /**
     * The number of mappings in this map.
     */
    private int size;
    
    /**
     * The modification count. Used for failing iteration over map that was 
     * modified during iteration via other than iterator methods.
     */
    private int modificationCount;
    
    /**
     * The bit mask for simulating modulo arithmetics.
     */
    private int moduloMask = keyNodes.length - 1;
    
    private final float loadFactor;
    private EntrySet entrySet = new EntrySet();
    
    public BidirectionalHashMap(float loadFactor, int capacity) {
        this.loadFactor = checkLoadFactor(loadFactor);
        capacity = fixCapacity(capacity);
        this.keyNodes = new KeyNode[capacity];
        this.valueNodes = new ValueNode[capacity];
    }
    
    public BidirectionalHashMap(float loadFactor) {
        this(loadFactor, DEFAULT_CAPACITY);
    }
    
    public BidirectionalHashMap(int capacity) {
        this(DEFAULT_LOAD_FACTOR, capacity);
    }
    
    public BidirectionalHashMap() {
        this(DEFAULT_LOAD_FACTOR, DEFAULT_CAPACITY);
    }
    
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override 
    public Collection<V> values() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    @Override 
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return accessKeyNode(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return accessValueNode(value) != null;
    }

    @Override
    public V get(Object key) {
        KeyNode<K, V> keyNode = accessKeyNode(key);
        return keyNode == null ? null : keyNode.mapping.value;
    }

    private void expand() {
        KeyNode<K, V>[] newKeyNodes = new KeyNode[keyNodes.length << 1];
        ValueNode<K, V>[] newValueNodes = new ValueNode[newKeyNodes.length];
        
        for (KeyNode<K, V> node = keyIterationHead; 
                node != null;
                node = node.down) {
            insertKeyNode(node, newKeyNodes);
        }
        
        for (ValueNode<K, V> node = valueIterationHead;
                node != null;
                node = node.down) {
            insertValueNode(node, newValueNodes);
        }
        
        this.keyNodes = newKeyNodes;
        this.valueNodes = newValueNodes;
        this.moduloMask = newKeyNodes.length - 1;
    }
    
    private void insertKeyNode(KeyNode<K, V> keyNode, 
                               KeyNode<K, V>[] newKeyNodes) {
        int newModuloMask = newKeyNodes.length - 1;
        int index = keyNode.mapping.keyHashCode & newModuloMask;
        
        if (newKeyNodes[index] == null) {
            newKeyNodes[index] = keyNode;
            keyNode.next = null;
        } else {
            keyNode.next = newKeyNodes[index];
            newKeyNodes[index].prev = keyNode;
            newKeyNodes[index] = keyNode;
        }
        
        keyNode.prev = null;
    }
    
    private void insertValueNode(ValueNode<K, V> valueNode,
                                 ValueNode<K, V>[] newValueNodes) {
        int newModuloMask = newValueNodes.length - 1;
        int index = valueNode.mapping.valueHashCode & newModuloMask;
        
        if (newValueNodes[index] == null) {
            newValueNodes[index] = valueNode;
            valueNode.next = null;
        } else {
            valueNode.next = newValueNodes[index];
            newValueNodes[index].prev = valueNode;
            newValueNodes[index] = valueNode;
        }
        
        valueNode.prev = null;
    }
    
    @Override
    public V put(K key, V value) {
        if (isFull()) {
            expand();
        }
        
        KeyNode<K, V> keyNode = accessKeyNode(key);
        V oldValue;
        
        if (keyNode == null) {
            putNonExisting(key, value);
            oldValue = null;
            size++;
        } else {
            oldValue = updateValue(keyNode, value);
        }
        
        modificationCount++;
        return oldValue;
    }

    @Override
    public V remove(Object key) {
        KeyNode<K, V> keyNode = accessKeyNode(key);
        
        if (keyNode == null) {
            return null;
        }
        
        size--;
        modificationCount++;
        return doRemove(keyNode);
    }
    
    @Override
    public void clear() {
        modificationCount += size;
        
        KeyNode<K, V> keyNode = keyIterationHead;
        
        while (keyNode != null) {
            int index = keyNode.mapping.keyHashCode & moduloMask;
            keyNodes[index] = null;
            keyNode = keyNode.next;
        }
        
        ValueNode<K, V> valueNode = valueIterationHead;
        
        while (valueNode != null) {
            int index = valueNode.mapping.valueHashCode & moduloMask;
            valueNodes[index] = null;
            valueNode = valueNode.next;
        }
        
        size = 0;
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }
    
    private final class EntrySet implements Set<Entry<K, V>> {

        private final class EntrySetIterator implements Iterator<Entry<K, V>> {
            
            private final int expectedModCount = modificationCount;
            private int iterated = 0;
            private KeyNode<K, V> entry = keyIterationHead;
            
            @Override
            public boolean hasNext() {
                checkModificationCount();
                return iterated < size;
            }

            @Override
            public Entry<K, V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                
                KeyNode<K, V> ret = entry;
                entry = entry.down;
                iterated++;
                return ret.mapping;
            }
            
            private void checkModificationCount() {
                if (expectedModCount != modificationCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
        
        @Override
        public int size() {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedMediaException();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedMediaException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean add(Entry<K, V> e) {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<K, V>> c) {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedMediaException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedMediaException();    
        }

        @Override
        public void clear() {
            throw new UnsupportedMediaException();
        }
    }
    
    private float checkLoadFactor(float loadFactor) {
        if (Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("The load factor is NaN.");
        }
        
        if (loadFactor <= 0.0f) {
            throw new IllegalArgumentException(
                    "The load factor is too small: " + loadFactor);
        }
        
        return loadFactor;
    }
    
    /**
     * Makes sure the capacity is no smaller than {@code MINIMUM_CAPACITY} and 
     * is a power of two.
     * 
     * @param capacity the requested capacity.
     * @return the actual capacity.
     */
    private int fixCapacity(int capacity) {
        capacity = Math.max(capacity, MINIMUM_CAPACITY);
        
        int actualCapacity = 1;
        
        while (actualCapacity < capacity) {
            actualCapacity <<= 1;
        }
        
        return actualCapacity;
    }

    private ValueNode<K, V> accessValueNode(Object value) {
        int inputValueHashCode = Objects.hashCode(value);
        int inputValueIndex = inputValueHashCode & moduloMask;
        
        for (ValueNode<K, V> node = valueNodes[inputValueIndex];
                node != null;
                node = node.next) {
            if (node.mapping.valueHashCode == inputValueHashCode 
                    && Objects.equals(node.mapping.value, value)) {
                return node;
            }
        }
        
        return null;
    }
    
    private ValueNode<K, V> accessValueNode(Object value, int valueHashCode) {
        int inputValueIndex = valueHashCode & moduloMask;
        
        for (ValueNode<K, V> node = valueNodes[inputValueIndex];
                node != null;
                node = node.next) {
            if (node.mapping.valueHashCode == valueHashCode
                    && Objects.equals(node.mapping.value, value)) {
                return node;
            }
        }
    
        return null;
    }
    
    private V updateValue(KeyNode<K, V> keyNode, V newValue) {
        V oldValue = keyNode.mapping.value;
        ValueNode<K, V> valueNode = 
                accessValueNode(newValue,
                                keyNode.mapping.valueHashCode);
        
        unlinkValueNodeFromIterationList(valueNode);
        appendValueNodeToIterationList(valueNode);
        unlinkValueNodeFromCollisionChain(valueNode);
        appendValueNodeToCollisionChain(valueNode, newValue);
        return oldValue;
    }
    
    private void appendValueNodeToCollisionChain(ValueNode<K, V> valueNode,
                                               V newValue) {
        int newValueHashCode = Objects.hashCode(newValue);
        int newValueIndex = newValueHashCode & moduloMask;
        
        if (valueNodes[newValueIndex] != null) {
            valueNodes[newValueIndex].prev = valueNode;
            valueNode.next = valueNodes[newValueIndex];
            valueNodes[newValueIndex] = valueNode;
        } else {
            valueNodes[newValueIndex] = valueNode;
        }
        
        valueNode.mapping.value = newValue;
        valueNode.mapping.valueHashCode = newValueHashCode;
    }
    
    private void unlinkKeyNodeFromCollisionChain(KeyNode<K, V> keyNode) {
        if (keyNode.prev != null) {
            keyNode.prev.next = keyNode.next;
        } else {
            int keyNodeIndex = keyNode.mapping.keyHashCode & moduloMask;
            keyNodes[keyNodeIndex] = keyNode.next;
            
            if (keyNodes[keyNodeIndex] != null) {
                keyNodes[keyNodeIndex].prev = null;
            }
        }
        
        if (keyNode.next != null) {
            keyNode.next.prev = keyNode.prev;
        }
    }
    
    private void unlinkValueNodeFromCollisionChain(ValueNode<K, V> valueNode) {
        if (valueNode.prev != null) {
            valueNode.prev.next = valueNode.next;
        } else {
            int valueNodeIndex = valueNode.mapping.valueHashCode & moduloMask;
            valueNodes[valueNodeIndex] = valueNode.next;
            
            if (valueNodes[valueNodeIndex] != null) {
                valueNodes[valueNodeIndex].prev = null;
            }
        }
        
        if (valueNode.next != null) {
            valueNode.next.prev = valueNode.prev;
        } 
    }
    
    private void unlinkKeyNodeFromIterationList(KeyNode<K, V> keyNode) {
        if (keyNode.up != null) {
            keyNode.up.down = keyNode.down;
        } else {
            keyIterationHead = keyIterationHead.down;
            
            if (keyIterationHead != null) {
                keyIterationHead.up = null;
            }
        }
        
        if (keyNode.down != null) {
            keyNode.down.up = keyNode.up;
        } else {
            keyIterationTail = keyIterationTail.up;
            
            if (keyIterationTail != null) {
                keyIterationTail.down = null;
            }
        }
    }
    
    private void unlinkValueNodeFromIterationList(ValueNode<K, V> valueNode) {
        if (valueNode.up != null) {
            valueNode.up.down = valueNode.down;
        } else {
            valueIterationHead = valueIterationHead.down;
            
            if (valueIterationHead != null) {
                valueIterationHead.up = null;
            }
        }
        
        if (valueNode.down != null) {
            valueNode.down.up = valueNode.up;
        } else {
            valueIterationTail = valueIterationTail.up;
            
            if (valueIterationTail != null) {
                valueIterationTail.down = null;
            } 
        }
    }
    
    private void appendValueNodeToIterationList(ValueNode<K, V> valueNode) {
        if (valueIterationTail != null) {
            valueIterationTail.next = valueNode;
            valueNode.prev = valueIterationTail;
            valueIterationTail = valueNode;
        } else {
            valueIterationHead = valueNode;
            valueIterationTail = valueNode;
        }
    }
    
    private void putNonExisting(K key, V value) {
        Mapping<K, V> mapping = new Mapping<>(key, value);
        KeyNode<K, V> keyNode = new KeyNode<>(mapping);
        ValueNode<K, V> valueNode = new ValueNode<>(mapping);
        
        // Link in the iteration list:
        if (size == 0) {
            keyIterationHead = keyNode;
            keyIterationTail = keyNode;
            
            valueIterationHead = valueNode;
            valueIterationTail = valueNode;
        } else {
            keyIterationTail.down = keyNode;
            keyNode.up = keyIterationTail;
            keyIterationTail = keyNode;
            
            valueIterationTail.down = valueNode;
            valueNode.up = valueIterationTail;
            valueIterationTail = valueNode;
        }
        
        // Add the key node and the value node to the beginning of their
        // respective collision chains:
        int keyIndex = mapping.keyHashCode & moduloMask;
        int valueIndex = mapping.valueHashCode & moduloMask;
        
        if (keyNodes[keyIndex] == null) {
            keyNodes[keyIndex] = keyNode;
        } else {
            keyNode.next = keyNodes[keyIndex];
            keyNodes[keyIndex].prev = keyNode;
            keyNodes[keyIndex] = keyNode;
        }
        
        if (valueNodes[valueIndex] == null) {
            valueNodes[valueIndex] = valueNode;
        } else {
            valueNode.next = valueNodes[valueIndex];
            valueNodes[valueIndex].prev = valueNode;
            valueNodes[valueIndex] = valueNode;
        }
    }
    
    private V doRemove(KeyNode<K, V> keyNode) {
        Mapping<K, V> mapping = keyNode.mapping;
        int valueHashCode = mapping.valueHashCode;
        ValueNode<K, V> valueNode = accessValueNode(mapping.value, 
                                                    mapping.valueHashCode);
        unlinkKeyNodeFromIterationList(keyNode);
        unlinkKeyNodeFromCollisionChain(keyNode);
        unlinkValueNodeFromIterationList(valueNode);
        unlinkValueNodeFromCollisionChain(valueNode);
        
        return mapping.value;
    }

    private KeyNode<K, V> accessKeyNode(Object key) {
        int inputKeyHashCode = Objects.hashCode(key);
        int inputKeyIndex = inputKeyHashCode & moduloMask;
        
        for (KeyNode<K, V> node = keyNodes[inputKeyIndex];
                node != null;
                node = node.next) {
            if (node.mapping.keyHashCode == inputKeyHashCode 
                    && Objects.equals(node.mapping.key, key)) {
                return node;
            }
        }
        
        return null;
    }
    
    private boolean isFull() {
        return size > (int)(loadFactor * keyNodes.length);
    }
}
