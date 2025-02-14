package com.royvanrijn.optimalopen;

import java.util.*;

/**
 * OptimalOpenHashMap implements Map using open addressing with linear probing.
 *
 * Based on:
 * "Optimal Bounds for Open Addressing Without Reordering"
 * by Martín Farach-Colton∗, Andrew Krapivin†, William Kuszmaul‡ (2025)
 *
 * This version uses lazy deletion via tombstones, cleaning up only when tombstones exceed a threshold.
 */
public class OptimalOpenHashMap<K, V> implements Map<K, V> {

    static final float DEFAULT_LOAD_FACTOR = 0.85f; // TODO tweak, should allow higher load
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16
    static final int MAXIMUM_CAPACITY = 1 << 30;

    // Unique tombstone marker.
    private static final Entry<?, ?> TOMBSTONE = new Entry<>(null, null, -1);

    private static class Entry<K, V> implements Map.Entry<K, V> {
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
    // Increase cleanup threshold to 50% to delay expensive cleanup.
    private static final float TOMBSTONE_THRESHOLD = 0.5f;

    /**
     * Table is a single array but we're filling it using the funnel method, in levels.
     *
     * @param key the key whose associated value is to be returned
     * @return
     */
    @Override
    public V get(Object key) {
        if (key == null) return null;
        int hash = hash(key);

        int idx = funnelProbe(key, hash);
        if(idx == -1) return null;
        Entry<K, V> e = table[idx];
        if (e == null)
            return null;
        if (e != TOMBSTONE && e.hash == hash && Objects.equals(e.key, key))
            return e.value;
        return null;
    }

    @Override
    public V put(K key, V value) {
        if(key == null) throw new NullPointerException("key is null");
        if ((size + tombstones + 1.0) / capacity > loadFactor) {
            if (capacity == MAXIMUM_CAPACITY) {
                throw new IllegalStateException("Cannot resize: maximum capacity reached (" + MAXIMUM_CAPACITY + ")");
            }
            resize();
        }
        int hash = hash(key);
        Entry<K, V>[] tab = table;
        int idx = funnelProbe(key, hash);
        if(idx != -1) {
            Entry<K, V> e = tab[idx];
            if (e == null) {
                tab[idx] = new Entry<>(key, value, hash);
                size++;
                return null;
            } else if (e.hash == hash && Objects.equals(e.key, key)) {
                V oldVal = e.value;
                e.value = value;
                return oldVal;
            }
        }
        resize();
        return put(key, value);
    }

    @Override
    public V remove(Object key) {
        if(key == null) throw new NullPointerException("key is null");
        int hash = hash(key);
        int idx = funnelProbe(key, hash);
        if(idx == -1) return null;
        Entry<K, V> e = table[idx];
        if (e == null)
            return null;
        if (e != TOMBSTONE && e.hash == hash && Objects.equals(e.key, key)) {
            V oldVal = e.value;
            table[idx] = (Entry<K, V>) TOMBSTONE;
            size--;
            tombstones++;
            if (((float) tombstones / capacity) > TOMBSTONE_THRESHOLD) {
                cleanup();
            }
            return oldVal;
        }
        return null;
    }

    /**
     * Rehashes the entire table to remove tombstones.
     */
    @SuppressWarnings("unchecked")
    private void cleanup() {
        Entry<K, V>[] oldTable = table;
        Entry<K, V>[] newTable = (Entry<K, V>[]) new Entry[capacity];
        int newSize = 0;
        for (Entry<K, V> e : oldTable) {
            if (e != null && e != TOMBSTONE) {
                int idx = funnelProbe(e.key, e.hash);
                if (newTable[idx] == null) {
                    newTable[idx] = e;
                    newSize++;
                    break;
                }
            }
        }
        table = newTable;
        size = newSize;
        tombstones = 0;
    }

    /**
     * Divide the space into blocks, for example a 128 table would have:
     * - 64 spots at level 1
     * - 32 spots at level 2
     * - 16 spots at level 3
     * - 8 spots at level 4
     * - 4 spots at level 5
     * - 2 spots at level 6
     * - 1 spot at level 7
     * - 1 spot at level 8
     *
     * Instead of going all the way down to 1 slot, we **stop after a fixed depth** and switch
     * to **linear probing** to avoid excessive probing depth.
     *
     * @param key   The key to probe for.
     * @param hash  The hashed value of the key.
     * @return      The computed index, or -1 if no available slot is found.
     */
    public int funnelProbe(Object key, int hash) {
        int levelWidth = capacity >>> 1; // first level size (half the table)
        int offset = 0;

        while (levelWidth > 0) {
            int localAttempt = hash & (levelWidth - 1);
            int idx = offset + localAttempt;

            Entry<K, V> entry = table[idx];
            if (entry == null || (entry != TOMBSTONE && entry.hash == hash && key.equals(entry.key))) {
                return idx;
            }

            // Move to next level
            offset += levelWidth;
            levelWidth >>>= 1;
        }

        return -1;
    }

    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
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
        int newSize = 0;
        tombstones = 0;
        for (Entry<K, V> e : oldTable) {
            if (e != null && e != TOMBSTONE) {
                int idx = funnelProbe(e.key, e.hash);
                table[idx] = e;
                newSize++;
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
    @Override public Set<Map.Entry<K, V>> entrySet() { throw new UnsupportedOperationException(); }
    @Override public Set<K> keySet() { throw new UnsupportedOperationException(); }
    @Override public Collection<V> values() { throw new UnsupportedOperationException(); }
}
