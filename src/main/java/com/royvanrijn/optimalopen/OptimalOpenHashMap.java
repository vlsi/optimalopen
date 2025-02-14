package com.royvanrijn.optimalopen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * OptimalOpenHashMap implements Map using open addressing with funnel probing.
 *
 * Based on:
 * "Optimal Bounds for Open Addressing Without Reordering"
 * by Martín Farach-Colton∗, Andrew Krapivin†, William Kuszmaul‡ (2025)
 *
 * This version uses lazy deletion via tombstones, cleaning up only when tombstones exceed a threshold (currently 25%)
 */
public class OptimalOpenHashMap<K, V> implements Map<K, V>, Serializable {

    static final float DEFAULT_LOAD_FACTOR = 0.9f; // In theory this algorithm supports higher load factors
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
        @Override public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (o instanceof Entry<?, ?> && hash != ((Entry<?, ?>) e).hash) {
                return false;
            }
            return Objects.equals(key, e.getKey()) &&
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

        int levelWidth = capacity >>> 1;
        int offset = 0;

        do {
            int levelIndex = hash & (levelWidth - 1);
            int tableIndex = offset + levelIndex;

            Entry<K, V> entry = table[tableIndex];
            if (entry == null) {
                return null;
            }
            if (entry != TOMBSTONE && entry.hash == hash && Objects.equals(entry.key, key)) {
                return entry.value;
            }
            // Move to next level
            offset |= levelWidth;
            levelWidth >>>= 1;

        } while(levelWidth > 0);

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

        int tombstoneIndex = -1; // fill gaps

        int levelWidth = capacity >>> 1;
        int offset = 0;

        do {
            int levelIndex = hash & (levelWidth - 1);
            int tableIndex = offset + levelIndex;

            // Actual put() logic:
            Entry<K, V> entry = table[tableIndex];
            if (entry == null || (entry != TOMBSTONE && entry.hash == hash && key.equals(entry.key))) {
                Entry<K, V> e = tab[tableIndex];
                if (e == null) {
                    if (tombstoneIndex != -1) {
                        // its a new entry, override the last tombstone:
                        tableIndex = tombstoneIndex;
                        tombstones--;
                    }
                    tab[tableIndex] = new Entry<>(key, value, hash);
                    size++;
                    return null;
                } else {
                    V oldVal = e.value;
                    e.value = value;
                    return oldVal;
                }
            } else if(entry == TOMBSTONE && tombstoneIndex == -1) {
                tombstoneIndex = tableIndex;
            }

            // Move to next level
            offset |= levelWidth;
            levelWidth >>>= 1;
        } while (levelWidth > 0);


        resize();
        return put(key, value);
    }

    @Override
    public V remove(Object key) {
        if(key == null) return null;
        int hash = hash(key);

        int levelWidth = capacity >>> 1;
        int offset = 0;

        do {
            int levelIndex = hash & (levelWidth - 1);
            int tableIndex = offset + levelIndex;

            Entry<K, V> entry = table[tableIndex];
            if (entry == null) {
                return null;
            }
            if (entry != TOMBSTONE && entry.hash == hash && Objects.equals(entry.key, key)) {
                V oldVal = entry.value;
                table[tableIndex] = TOMBSTONE;
                size--;
                tombstones++;
                // If the amount of tombstones becomes larger than 25%, clean up:
                if (tombstones > capacity>>2) {
                    cleanup();
                }
                return oldVal;
            }

            // Move to next level
            offset |= levelWidth;
            levelWidth >>>= 1;

        } while (levelWidth > 0);

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
     * Divide the available space into blocks, for example a 128-capacity table in our case will have:
     * - 64 spots at level 1
     * - 32 spots at level 2
     * - 16 spots at level 3
     * - 8 spots at level 4
     * - 4 spots at level 5
     * - 2 spots at level 6
     * - 1 spot at level 7
     * - 1 spot at level 8
     *
     * This is probably not optimal, but fast to implement on the CPU.
     *
     * @param key   The key to probe for.
     * @param hash  The hashed value of the key.
     * @return      The computed index, or -1 if no available slot is found.
     */
    public int funnelProbe(Object key, int hash) {

        int levelWidth = capacity >>> 1;
        int offset = 0;

        do {
            int levelIndex = hash & (levelWidth - 1);
            int tableIndex = offset + levelIndex;

            Entry<K, V> entry = table[tableIndex];
            if (entry == null || (entry != TOMBSTONE && entry.hash == hash && key.equals(entry.key))) {
                // to avoid doing this twice this funnelProbe has been inlined into get(), put(), remove().
                return tableIndex;
            }

            // Move to next level
            offset |= levelWidth;
            levelWidth >>>= 1;

        } while (levelWidth > 0);

        // not found.
        return -1;
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

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        // TODO: rework it so modifications to the entry set are reflected in the map (e.g. remove)
        Set<Map.Entry<K, V>> set = new LinkedHashSet<>();
        for (Entry<K, V> e : table) {
            if (e != null && e != TOMBSTONE) {
                set.add(e);
            }
        }
        return set;
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = new LinkedHashSet<>();
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
    public int hashCode() {
        int hash = 0;
        for (Map.Entry<K, V> entry : entrySet()) {
            hash += entry.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Map<?, ?>)) {
            return false;
        }

        Map<?, ?> that = (Map<?, ?>) o;

        if (that.size() != size()) {
            return false;
        }

        for (Map.Entry<K, V> e : entrySet()) {
            K key = e.getKey();
            V value = e.getValue();
            if (value == null) {
                if (!(that.get(key) == null && that.containsKey(key))) {
                    return false;
                }
            } else if (!value.equals(that.get(key))) {
                return false;
            }
        }

        return true;
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
