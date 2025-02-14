package com.royvanrijn.optimalopen;

import java.util.HashMap;
import java.util.Map;

public class MapPerformanceTest {

    private static final int NUM_OPERATIONS = 10_000_000;
    private static final int WARMUP_OPERATIONS = 1_000_000;

    public static void main(String[] args) {

        for(int times = 0; times < 3; times++) {
            // 100% not the right way to do performance testing, JMH much better:

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

            System.out.println("---------------------------------------");
            // Warmup and test for LinearHashMap:
            System.out.println("Warming up LinearOpenHashMap...");
            for (int i = 0; i < 5; i++) {
                warmupMap(new LinearOpenHashMap<>());
            }
            long linearMapTime = fullBenchmarkMap(new LinearOpenHashMap<>());
            System.out.println("LinearOpenHashMap full benchmark total time: " + linearMapTime + " ms");

        }
    }

    /**
     * Warmup: use fewer operations to let the JIT optimize.
     */
    private static void warmupMap(Map<String, Integer> map) {
        for (int i = 0; i < WARMUP_OPERATIONS; i++) {
            map.put("key:"+i, i);
        }
        for (int i = 0; i < WARMUP_OPERATIONS; i++) {
            if (map.get("key:"+i) != i) {
                throw new AssertionError("Incorrect value retrieved during warmup!");
            }
        }
        map.clear();
    }

    /**
     * Full benchmark: insert NUM_OPERATIONS keys, then get them,
     * then delete half the keys, and return the total elapsed time.
     */
    private static long fullBenchmarkMap(Map<String, Integer> map) {
        long startTime = System.currentTimeMillis();

        // Insertion phase
        for (int i = 0; i < NUM_OPERATIONS; i++) {
            map.put("key:"+i, i);
        }
        long insertionTime = System.currentTimeMillis();
        System.out.println("Insertion time: " + (insertionTime - startTime) + " ms");

        // Retrieval phase
        for (int i = 0; i < NUM_OPERATIONS; i++) {
            if (map.get("key:"+i) != i) {
                throw new AssertionError("Incorrect value retrieved during get for key: " + i);
            }
        }
        long retrievalTime = System.currentTimeMillis();
        System.out.println("Retrieval time: " + (retrievalTime - insertionTime) + " ms");

        // Deletion phase: remove even keys.
        for (int i = 0; i < NUM_OPERATIONS; i += 2) {
            map.remove("key:" + i);
        }
        long deletionTime = System.currentTimeMillis();
        System.out.println("Deletion time: " + (deletionTime - retrievalTime) + " ms");

        return deletionTime - startTime;
    }
}
