package com.royvanrijn.optimalopen;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Map;

@State(Scope.Benchmark)
public class JMHBenchmark {

    private static final int NUM_KEYS = 100_000;

    @Param({"JDK", "OPTIMAL"})
    private String mapType;
    private Map<Integer, Integer> unchangingMap;

    @Setup(Level.Trial)
    public void setup() {
        unchangingMap = fillMap(createMap());
    }

    private static Map<Integer, Integer> fillMap(Map<Integer, Integer> map) {
        for (var i = 0; i < NUM_KEYS; i++) {
            map.put(i, i);
        }
        return map;
    }

    private Map<Integer, Integer> createMap() {
        return switch (mapType) {
            case "JDK" -> new HashMap<>();
            case "OPTIMAL" -> new OptimalOpenHashMap<>();
            default -> throw new IllegalArgumentException("Unknown map type: " + mapType);
        };
    }

    @Benchmark
    public Map<Integer, Integer> put(Blackhole blackhole) {
        var map = createMap(); // Pay small price for instantiating the map.
        for (var i = 0; i < NUM_KEYS; i++) {
            var key = Integer.valueOf(i); // Box just once, not twice.
            blackhole.consume(map.put(key, key));
        }
        return map;
    }

    @Benchmark
    public Map<Integer, Integer> get(Blackhole blackhole) {
        for (var i = 0; i < NUM_KEYS; i++) {
            blackhole.consume(unchangingMap.get(i));
        }
        return unchangingMap;
    }

    @Benchmark
    public Map<Integer, Integer> remove(Blackhole blackhole) {
        // We can't use the unchanging map here, because we need to remove elements from it.
        // And because the map needs to start from the same starting point each time,
        // we need to recreate it before each trial.
        // Since JMH discourages trial-level @Setup, we have to incur the cost of filling the map here.
        // Therefore the remove() benchmark is heavily affected by put() operations, compromising it.
        var map = fillMap(createMap());
        for (var i = 0; i < NUM_KEYS; i++) {
            if (i % 2 == 1) { // Remove every even key.
                continue;
            }
            blackhole.consume(map.remove(i));
        }
        return map;
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

}
