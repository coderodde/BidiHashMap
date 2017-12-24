package net.coderodde.util;

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
        
        @Override
        public String toString() {
            return "[" + Objects.toString(key) 
                       + " <-> " 
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
     * The minimum load factor.
     */
    private static final float MINIMUM_LOAD_FACTOR = 0.1f;
    
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
    
    /**
     * The load factor.
     */
    private final float loadFactor;
    
    /**
     * The entry set.
     */
    private final EntrySet entrySet = new EntrySet();
    
    /**
     * The inverse map.
     */
    private final InverseMap inverseMap = new InverseMap();
    
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
    public boolean containsKey(Object key) {
        return accessKeyNode(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return accessValueNode(value) != null;
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }

    @Override
    public V get(Object key) {
        KeyNode<K, V> keyNode = accessKeyNode(key);
        return keyNode == null ? null : keyNode.mapping.value;
    }
    
    public Map<V, K> inverseMap() {
        return inverseMap;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
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
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
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
    public int size() {
        return size;
    }

    @Override 
    public Collection<V> values() {
        throw new UnsupportedOperationException(); 
    }

    /**
     * Attempts to access the key node containing the input key.
     * 
     * @param key the target key.
     * @return a key node containing the given key or {@code null} if there is
     *         no such.
     */
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
    
    /**
     * Attempts to access the key node containing the given key. This version
     * does not compute the hash code of the input key but rather uses the hash
     * code given in the second argument.
     * 
     * @param key         the target key.
     * @param keyHashCode the hash code of the key.
     * @return a key node containing the given key or {@code null} if there is
     *         no such.
     */
    private KeyNode<K, V> accessKeyNode(Object key, int keyHashCode) {
        int inputKeyIndex = keyHashCode & moduloMask;
        
        for (KeyNode<K, V> node = keyNodes[inputKeyIndex];
                node != null;
                node = node.next) {
            if (node.mapping.keyHashCode == keyHashCode
                    && Objects.equals(node.mapping.key, key)) {
                return node;
            }
        }
        
        return null;
    }

    /**
     * Attempts to access the value node containing the input value.
     * 
     * @param value the target value.
     * @return a value node containing the given value or {@code null} if there 
     *         is no such.
     */
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
    
    /**
     * Attempts to access the value node containing the given value. This 
     * version does not compute the hash code of the input value but rather uses 
     * the hash code given in the second argument.
     * 
     * @param value         the target value.
     * @param valueHashCode the hash code of the value.
     * @return a value node containing the given value or {@code null} if there
     *         is no such.
     */
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
    
    /**
     * Appends {@code valueNode} to the tail of the value node iteration list.
     * 
     * @param valueNode the value node to append.
     */
    private void appendValueNodeToIterationList(ValueNode<K, V> valueNode) {
        if (valueNode.up != null) {
            System.out.println("up");
        }

        if (valueNode.down != null) {
            System.out.println("down");
        }
            
        if (valueIterationTail != null) {
            valueIterationTail.down = valueNode;
            valueNode.up = valueIterationTail;
            valueIterationTail = valueNode;
            valueNode.down = null;
        } else {
            valueIterationHead = valueNode;
            valueIterationTail = valueNode;
            valueNode.up = null;
            valueNode.down = null;
        }
    }
    
    /**
     * Checks the load factor.
     * 
     * @param loadFactor the candidate load factor.
     * @return the input load factor.
     * @throws IllegalArgumentException if the input load factor is too small or
     *                                  is a NaN value.
     */
    private float checkLoadFactor(float loadFactor) {
        if (Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("The load factor is NaN.");
        }
        
        if (loadFactor <= MINIMUM_LOAD_FACTOR) {
            throw new IllegalArgumentException(
                    "The load factor is too small: " + loadFactor + ". " +
                    "Must be at least " + MINIMUM_LOAD_FACTOR + ".");
        }
        
        return loadFactor;
    }
    
    /**
     * Removes the key node and its related mapping and value node.
     * 
     * @param keyNode the key node to remove.
     * @return the value of the removed mapping.
     */
    private V doRemove(KeyNode<K, V> keyNode) {
        Mapping<K, V> mapping = keyNode.mapping;
        ValueNode<K, V> valueNode = accessValueNode(mapping.value, 
                                                    mapping.valueHashCode);
        unlinkKeyNodeFromIterationList(keyNode);
        unlinkKeyNodeFromCollisionChain(keyNode);
        unlinkValueNodeFromIterationList(valueNode);
        unlinkValueNodeFromCollisionChain(valueNode);
        
        return mapping.value;
    }

    /**
     * Makes the internal key and value tables twice as large as they are and
     * relinks all the mappings to the new larger tables.
     */
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
    
    /**
     * Inserts the given key node to its correct location in 
     * {@code newKeyNodes}.
     * 
     * @param keyNode     the key node to insert.
     * @param newKeyNodes the new key node table.
     */
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
    
    /**
     * Inserts the given value node to its correct location in 
     * {@code newValueNodes}.
     * 
     * @param valueNode     the value node to insert.
     * @param newValueNodes the new value node table.
     */
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
    
    /**
     * Returns {@code true} if the data structure is sufficiently large for
     * making the internal tables larger.
     * 
     * @return {@code true} if the data structure should expand.
     */
    private boolean isFull() {
        return size > (int)(loadFactor * keyNodes.length);
    }
    
    /**
     * Prepends {@code valueNode} to the head of a collision chain of 
     * {@code newValue}.
     * 
     * @param valueNode the target value node.
     * @param newValue  the new value for the value node.
     */
    private void prependValueNodeToCollisionChain(ValueNode<K, V> valueNode,
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
    
    /**
     * Inserts a new non-existent key/value mapping to this hash map.
     * @param key   the key of the mapping.
     * @param value the value of the mapping.
     */
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
    
    /**
     * Removes the given key node from the key iteration list.
     * 
     * @param keyNode the target key node to remove.
     */
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
    
    /**
     * Removes the given key node from its current collision chain.
     * 
     * @param keyNode the target key node to remove.
     */
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
    
    /**
     * Removes the given value node from the value iteration list.
     * 
     * @param valueNode the target value node to remove.
     */
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
    
    /**
     * Removes the given value node from its current collision chain.
     * 
     * @param valueNode the target value node to remove.
     */
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
    
    /**
     * Updates the value associated with {@code keyNode}.
     * 
     * @param keyNode  the target key node.
     * @param newValue the new value for the key node.
     * @return the old value associated with the given key node.
     */
    private V updateValue(KeyNode<K, V> keyNode, V newValue) {
        V oldValue = keyNode.mapping.value;
        ValueNode<K, V> valueNode = 
                accessValueNode(newValue,
                                keyNode.mapping.valueHashCode);
        
        unlinkValueNodeFromIterationList(valueNode);
        appendValueNodeToIterationList(valueNode);
        unlinkValueNodeFromCollisionChain(valueNode);
        prependValueNodeToCollisionChain(valueNode, newValue);
        return oldValue;
    }
    
    /**
     * This class implements the inverse view mapping values to keys.
     */
    private final class InverseMap implements Map<V, K> {

        @Override
        public void clear() {
            throw new UnsupportedOperationException(); 
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<V, K>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public K get(Object value) {
            ValueNode<K, V> valueNode = accessValueNode(value);
            return valueNode != null ? valueNode.mapping.key : null;
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException(); 
        }

        @Override
        public Set<V> keySet() {
            throw new UnsupportedOperationException(); 
        }

        @Override
        public K put(V value, K key) {
            if (isFull()) {
                expand();
            }
            
            ValueNode<K, V> valueNode = accessValueNode(value);
            K oldKey;
            
            if (valueNode == null) {
                putNonExisting(key, value);
                oldKey = null;
                size++;
            } else {
                oldKey = updateKey(valueNode, key);
            }
            
            modificationCount++;
            return oldKey;
        }
        
        @Override
        public void putAll(Map<? extends V, ? extends K> m) {
            for (Map.Entry<? extends V, ? extends K> e : m.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        }
        
        @Override
        public K remove(Object value) {
            ValueNode<K, V> valueNode = accessValueNode(value);
            
            if (valueNode == null) {
                return null;
            }
            
            size--;
            modificationCount++;
            return doRemove(valueNode);
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<K> values() {
            throw new UnsupportedOperationException(); 
        }

        /**
         * Appends the given key node to the tail of the key iteration list.
         * 
         * @param keyNode the target key node to append.
         */
        private void appendKeyNodeToIterationList(KeyNode<K, V> keyNode) {
            if (keyIterationTail != null) {
                keyIterationTail.down = keyNode;
                keyNode.up = keyIterationTail;
                keyIterationTail = keyNode;
                keyNode.down = null;
            } else {
                keyIterationHead = keyNode;
                keyIterationTail = keyNode;
                keyNode.up = null;
                keyNode.down = null;
            }
        }
        
        /**
         * Removes the value node and its related mapping and key node from this
         * data structure.
         * 
         * @param valueNode the target value node.
         * @return the key of the mapping removed.
         */
        private K doRemove(ValueNode<K, V> valueNode) {
            Mapping<K, V> mapping = valueNode.mapping;
            KeyNode<K, V> keyNode = accessKeyNode(mapping.key,
                                                  mapping.keyHashCode);
            
            unlinkKeyNodeFromIterationList(keyNode);
            unlinkKeyNodeFromCollisionChain(keyNode);
            unlinkValueNodeFromIterationList(valueNode);
            unlinkValueNodeFromCollisionChain(valueNode);
            
            return mapping.key;
        }
        
        /**
         * Inserts the given key node to the beginning of a collision chain 
         * associated with {@code newKey}.
         * 
         * @param keyNode the target key node.
         * @param newKey  the new key.
         */
        private void prependKeyNodeToCollisionChain(KeyNode<K, V> keyNode, 
                                                    K newKey) {
            int newKeyHashCode = Objects.hashCode(newKey);
            int newKeyIndex = newKeyHashCode & moduloMask;
            
            if (keyNodes[newKeyIndex] != null) {
                keyNodes[newKeyIndex].prev = keyNode;
                keyNode.next = keyNodes[newKeyIndex];
                keyNodes[newKeyIndex] = keyNode;
            } else {
                keyNodes[newKeyIndex] = keyNode;
            }
            
            keyNode.mapping.key = newKey;
            keyNode.mapping.keyHashCode = newKeyHashCode;
        }
        
        /**
         * Updates the key associated with the given value node.
         * 
         * @param valueNode the target value node.
         * @param newKey    the new key.
         * @return the old key.
         */
        private K updateKey(ValueNode<K, V> valueNode, K newKey) {
            K oldKey = valueNode.mapping.key;
            KeyNode<K, V> keyNode = 
                    accessKeyNode(newKey, valueNode.mapping.keyHashCode);
            
            unlinkKeyNodeFromIterationList(keyNode);
            appendKeyNodeToIterationList(keyNode);
            unlinkKeyNodeFromCollisionChain(keyNode);
            prependKeyNodeToCollisionChain(keyNode, newKey);
            return oldKey;
        }
    }
    
    /**
     * This inner class implements a view over entries.
     */
    private final class EntrySet implements Set<Entry<K, V>> {

        /**
         * This inner class implements an iterator over a set of entries.
         */
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
                checkModificationCount();
                
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
        public boolean add(Entry<K, V> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();    
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }
    }
}
