package com.royvanrijn.optimalopen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class OptimalOpenHashMapTest {

    @Test
    public void testPutAndGet() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
    }

    @Test
    public void testOverwriteValue() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        map.put("key", 1);
        map.put("key", 2);
        assertEquals(2, map.get("key"));
    }

    @Test
    public void testRemove() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        map.put("key", 1);
        assertEquals(1, map.remove("key"));
        assertNull(map.get("key"));
    }

    @Test
    public void testContainsKey() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        map.put("key", 1);
        assertTrue(map.containsKey("key"));
        assertFalse(map.containsKey("nonexistent"));
    }

    @Test
    public void testIsEmptyAndSize() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        assertTrue(map.isEmpty());
        map.put("key", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
    }

    @Test
    public void testResizeAndRehash() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // Insert enough keys to force a resize.
        int total = 200; // adjust to force rehashing beyond initial capacity.
        for (int i = 0; i < total; i++) {
            map.put("key" + i, i);
        }
        for (int i = 0; i < total; i++) {
            assertEquals(i, map.get("key" + i));
        }
        assertEquals(total, map.size());
    }

    @Test
    public void testPutAllAndClear() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        map.putAll(source);

        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("a"));
    }

    @Test
    public void testRandomOperationsComparison() {
        // We'll perform a sequence of random operations on both maps and then compare their behavior.
        Map<String, Integer> jdkMap = new HashMap<>();
        OptimalOpenHashMap<String, Integer> optMap = new OptimalOpenHashMap<>();
        Random rand = new Random(42);
        int operations = 1000;
        int keySpace = 100; // keys will be "key0" .. "key99"

        for (int i = 0; i < operations; i++) {
            int op = rand.nextInt(3);
            String key = "key" + rand.nextInt(keySpace);
            if (op == 0) { // put operation
                int value = rand.nextInt(1000);
                Integer resJdk = jdkMap.put(key, value);
                Integer resOpt = optMap.put(key, value);
                assertEquals(resJdk, resOpt, "Mismatch in put() for key: " + key);
            } else if (op == 1) { // get operation
                Integer resJdk = jdkMap.get(key);
                Integer resOpt = optMap.get(key);
                assertEquals(resJdk, resOpt, "Mismatch in get() for key: " + key);
            } else { // remove operation
                Integer resJdk = jdkMap.remove(key);
                Integer resOpt = optMap.remove(key);
                assertEquals(resJdk, resOpt, "Mismatch in remove() for key: " + key);
            }
        }
        // Final consistency: size and values should be the same.
        assertEquals(jdkMap.size(), optMap.size(), "Final map sizes differ");
        for (String k : jdkMap.keySet()) {
            assertEquals(jdkMap.get(k), optMap.get(k), "Mismatch for key: " + k);
        }
    }


    @Test
    public void testNullKeyGet() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // get(null) is defined to return null.
        assertNull(map.get(null));
    }

    @Test
    public void testNullKeyPut() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // put(null, value) should throw a NullPointerException
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
    }

    @Test
    public void testNullKeyRemove() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // remove(null) should throw a NullPointerException
        assertThrows(NullPointerException.class, () -> map.remove(null));
    }

    @Test
    public void testPutAllNull() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // putAll(null) should throw a NullPointerException
        assertThrows(NullPointerException.class, () -> map.putAll(null));
    }

    @Test
    public void testClearResetsCapacity() throws Exception {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.clear();

        // Verify that after clear() the table's length is reset to DEFAULT_INITIAL_CAPACITY.
        Field tableField = map.getClass().getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
        assertEquals(OptimalOpenHashMap.DEFAULT_INITIAL_CAPACITY, table.length);
    }


    @Test
    public void testPutTombstoneBehavior() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // Insert a key, then remove it to create a tombstone.
        map.put("a", 100);
        assertEquals(100, map.remove("a"));
        // Now, putting the same key should eventually find the key if it exists further down the probe sequence.
        // Insert a different key that might occupy an earlier slot.
        map.put("b", 200);
        // Now reinsert "a". Even if a tombstone was encountered, the algorithm should continue probing and update
        // if the key already exists later.
        // First insertion returns null (since "a" is logically absent).
        assertNull(map.put("a", 300));
        // Now update "a": put should return the old value 300.
        assertEquals(300, map.put("a", 400));
        assertEquals(400, map.get("a"));
    }

    @Test
    public void testMaxCapacityExceeded() throws Exception {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // Use reflection to set the internal table to a maximum capacity table.
        Field tableField = OptimalOpenHashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Class<?> compType = tableField.getType().getComponentType();
        Object newTable = Array.newInstance(compType, OptimalOpenHashMap.MAXIMUM_CAPACITY);
        tableField.set(map, newTable);

        // Set size to simulate a load factor above threshold.
        Field sizeField = map.getClass().getDeclaredField("size");
        sizeField.setAccessible(true);
        int simulatedSize = (int)(OptimalOpenHashMap.MAXIMUM_CAPACITY * OptimalOpenHashMap.DEFAULT_LOAD_FACTOR);
        sizeField.set(map, simulatedSize);

        // Now, a put() should try to resize and throw an exception.
        assertThrows(IllegalStateException.class, () -> map.put("exceed", 999));
    }

    @Test
    public void testResizeAndRehashNoDuplicates() {
        OptimalOpenHashMap<String, Integer> map = new OptimalOpenHashMap<>();
        // Insert keys to force at least one resize.
        int total = 200;
        for (int i = 0; i < total; i++) {
            map.put("key" + i, i);
        }
        // Now update one key that was already inserted.
        // For a key that already exists, put() should return its previous value.
        Integer prev = map.put("key52", 366);
        // Expect that the key "key52" was present; previous value must not be null.
        assertNotNull(prev, "Mismatch in put() for key: key52");
        // And now the value should be updated.
        assertEquals(366, map.get("key52"));
    }
}
