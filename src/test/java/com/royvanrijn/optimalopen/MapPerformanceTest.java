package com.royvanrijn.optimalopen;

import java.util.HashMap;
import java.util.Map;

public class MapPerformanceTest {

    private static final int NUM_OPERATIONS = 100_000_000;
    private static final int WARMUP_OPERATIONS = 1_000_000;

    public static void main(String[] args) {
        System.out.println("Starting performance tests...");

        // Warmup and test for HashMap:
        System.out.println("Warming up HashMap...");
        for (int i = 0; i < 5; i++) {
            warmupMap(new HashMap<>());
        }
        long hashMapTime = fullBenchmarkMap(new HashMap<>());
        System.out.println("HashMap full benchmark total time: " + hashMapTime + " ms");
        System.out.println("---------------------------------------");

        // Warmup and test for OptimalOpenHashMap:
        System.out.println("Warming up OptimalOpenHashMap...");
        for (int i = 0; i < 5; i++) {
            warmupMap(new OptimalOpenHashMap<>());
        }
        long optimalMapTime = fullBenchmarkMap(new OptimalOpenHashMap<>());
        System.out.println("OptimalOpenHashMap full benchmark total time: " + optimalMapTime + " ms");
    }

    /**
     * Warmup: use fewer operations to let the JIT optimize.
     */
    private static void warmupMap(Map<Integer, Integer> map) {
        for (int i = 0; i < WARMUP_OPERATIONS; i++) {
            map.put(i, i);
        }
        for (int i = 0; i < WARMUP_OPERATIONS; i++) {
            if (map.get(i) != i) {
                throw new AssertionError("Incorrect value retrieved during warmup!");
            }
        }
        map.clear();
    }

    /**
     * Full benchmark: insert NUM_OPERATIONS keys, then get them,
     * then delete half the keys, and return the total elapsed time.
     */
    private static long fullBenchmarkMap(Map<Integer, Integer> map) {
        long startTime = System.currentTimeMillis();

        // Insertion phase
        for (int i = 0; i < NUM_OPERATIONS; i++) {
            map.put(i, i);
        }
        long insertionTime = System.currentTimeMillis();
        System.out.println("Insertion time: " + (insertionTime - startTime) + " ms");

        // Retrieval phase
        for (int i = 0; i < NUM_OPERATIONS; i++) {
            if (map.get(i) != i) {
                throw new AssertionError("Incorrect value retrieved during get for key: " + i);
            }
        }
        long retrievalTime = System.currentTimeMillis();
        System.out.println("Retrieval time: " + (retrievalTime - insertionTime) + " ms");

        // Deletion phase: remove even keys.
        for (int i = 0; i < NUM_OPERATIONS; i += 2) {
            map.remove(i);
        }
        long deletionTime = System.currentTimeMillis();
        System.out.println("Deletion time: " + (deletionTime - retrievalTime) + " ms");

        return deletionTime - startTime;
    }
}
