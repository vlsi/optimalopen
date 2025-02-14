package com.royvanrijn.optimalopen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * LinearHashMap implements Map using open addressing with linear probing.
 *
 * Used as basis to test a variation of the optimal open addressing.
 *
 * This version uses lazy deletion via tombstones, cleaning up only when tombstones exceed a threshold (currently 25%)
 */
public class LinearOpenHashMap<K, V> implements Map<K, V>, Serializable {

    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16
    static final int MAXIMUM_CAPACITY = 1 << 30;

    // Unique tombstone marker.
    private final Entry<K, V> TOMBSTONE = new Entry<>(null, null, -1);

    private static class Entry<K, V> implements Map.Entry<K, V>, Serializable {
        final K key;
        V value;
        final int hash; // cached hash value

        Entry(K key, V value, int hash) {
            this.key = key;
            this.value = value;
            this.hash = hash;
        }
        @Override public K getKey() { return key; }
        @Override public V getValue() { return value; }
        @Override public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }
        @Override public int hashCode() { return hash ^ Objects.hashCode(value); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return hash == ((Entry<?, ?>) e).hash &&
                    Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue());
        }
        @Override public String toString() { return key + "=" + value; }
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] table = (Entry<K, V>[]) new Entry[DEFAULT_INITIAL_CAPACITY];
    private int size = 0;
    private int capacity = DEFAULT_INITIAL_CAPACITY;
    private final float loadFactor = DEFAULT_LOAD_FACTOR;
    // Count of tombstones in the table.
    private int tombstones = 0;

    @Override
    public V get(Object key) {
        if (key == null)
            return null;
        int hash = hash(key);
        int mask = capacity - 1;
        for (int i = 0; i < capacity; i++) {
            int idx = nextIndex(hash, i, mask);
            Entry<K, V> e = table[idx];
            if (e == null)
                return null;
            if (e != TOMBSTONE && e.hash == hash && Objects.equals(e.key, key))
                return e.value;
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        if ((size + tombstones + 1.0) / capacity > loadFactor) {
            if (capacity == MAXIMUM_CAPACITY) {
                throw new IllegalStateException("Cannot resize: maximum capacity reached (" + MAXIMUM_CAPACITY + ")");
            }
            resize();
        }
        int hash = hash(key);
        int mask = capacity - 1;
        int tombstoneIndex = -1;
        for (int i = 0; i < capacity; i++) {
            int idx = nextIndex(hash, i, mask);
            Entry<K, V> e = table[idx];
            if (e == null) {
                if (tombstoneIndex != -1) {
                    idx = tombstoneIndex;
                    tombstones--;
                }
                table[idx] = new Entry<>(key, value, hash);
                size++;
                return null;
            } else if (e == TOMBSTONE) {
                if (tombstoneIndex == -1)
                    tombstoneIndex = idx;
            } else if (e.hash == hash && Objects.equals(e.key, key)) {
                V oldVal = e.value;
                e.value = value;
                return oldVal;
            }
        }
        throw new IllegalStateException("Table is full");
    }

    @Override
    public V remove(Object key) {
        if(key == null) return null;
        int hash = hash(key);
        int mask = capacity - 1;
        for (int i = 0; i < capacity; i++) {
            int idx = nextIndex(hash, i, mask);
            Entry<K, V> e = table[idx];
            if (e == null)
                return null;
            if (e != TOMBSTONE && e.hash == hash && Objects.equals(e.key, key)) {
                V oldVal = e.value;
                table[idx] = TOMBSTONE;
                size--;
                tombstones++;
                if (tombstones > capacity >> 2) {
                    cleanup();
                }
                return oldVal;
            }
        }
        return null;
    }

    /**
     * Rehashes the entire table to remove tombstones.
     */
    @SuppressWarnings("unchecked")
    private void cleanup() {
        Entry<K, V>[] oldTable = table;
        int mask = capacity - 1;
        Entry<K, V>[] newTable = (Entry<K, V>[]) new Entry[capacity];
        int newSize = 0;
        for (Entry<K, V> e : oldTable) {
            if (e != null && e != TOMBSTONE) {
                for (int i = 0; i < capacity; i++) {
                    int idx = nextIndex(e.hash, i, mask);
                    if (newTable[idx] == null) {
                        newTable[idx] = e;
                        newSize++;
                        break;
                    }
                }
            }
        }
        table = newTable;
        size = newSize;
        tombstones = 0;
    }

    // Simple linear probing:
    private int nextIndex(int baseHash, int attempt, int mask) {
        return (baseHash + attempt) & mask;
    }

    static final int hash(Object key) {
        int h;
        return (h = key.hashCode()) ^ (h >>> 16);
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        if (capacity >= MAXIMUM_CAPACITY) {
            throw new IllegalStateException("Cannot resize: maximum capacity reached (" + MAXIMUM_CAPACITY + ")");
        }
        int newCapacity = capacity << 1;
        if (newCapacity > MAXIMUM_CAPACITY)
            newCapacity = MAXIMUM_CAPACITY;
        Entry<K, V>[] oldTable = table;
        table = (Entry<K, V>[]) new Entry[newCapacity];
        capacity = newCapacity;
        int mask = capacity - 1;
        int newSize = 0;
        tombstones = 0;
        for (Entry<K, V> e : oldTable) {
            if (e != null && e != TOMBSTONE) {
                for (int i = 0; i < capacity; i++) {
                    int idx = (e.hash + i) & mask;
                    if (table[idx] == null) {
                        table[idx] = e;
                        newSize++;
                        break;
                    }
                }
            }
        }
        size = newSize;
    }

    @Override
    public void clear() {
        @SuppressWarnings("unchecked")
        Entry<K, V>[] newTable = (Entry<K, V>[]) new Entry[DEFAULT_INITIAL_CAPACITY];
        table = newTable;
        size = 0;
        capacity = DEFAULT_INITIAL_CAPACITY;
        tombstones = 0;
    }

    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }
    @Override public boolean containsKey(Object key) { return get(key) != null; }
    @Override public boolean containsValue(Object value) {
        for (Entry<K, V> e : table) {
            if (e != null && e != TOMBSTONE && Objects.equals(e.value, value))
                return true;
        }
        return false;
    }

    @Override public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = new HashSet<>();
        for (Entry<K, V> e : table) {
            if (e != null && e != TOMBSTONE) {
                set.add(e);
            }
        }
        return set;
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = new HashSet<>();
        for (Entry<K, V> e : table) {
            if (e != null && e != TOMBSTONE) {
                set.add(e.getKey());
            }
        }
        return set;
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (Entry<K, V> e : table) {
            if (e != null && e != TOMBSTONE) {
                values.add(e.getValue());
            }
        }
        return values;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Entry<K, V> e : table) {
            if (e != null && e != TOMBSTONE) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(e.key).append("=").append(e.value);
                first = false;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
