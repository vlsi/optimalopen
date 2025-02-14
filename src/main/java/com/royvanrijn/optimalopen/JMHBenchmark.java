package com.royvanrijn.optimalopen;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class JMHBenchmark {

    private static final int NUM_KEYS = 1_000_000;

    @Param({"JDK", "OPTIMAL", "LINEAR"})
    private String mapType;
    private Map<String, Integer> unchangingMap;

    @Setup(Level.Trial)
    public void setup() {
        unchangingMap = fillMap(createMap());
    }

    private static Map<String, Integer> fillMap(Map<String, Integer> map) {
        for (var i = 0; i < NUM_KEYS; i++) {
            map.put("key:"+i, i);
        }
        return map;
    }

    private Map<String, Integer> createMap() {
        return switch (mapType) {
            case "JDK" -> new HashMap<>();
            case "OPTIMAL" -> new OptimalOpenHashMap<>();
            case "LINEAR" -> new LinearOpenHashMap<>();
            default -> throw new IllegalArgumentException("Unknown map type: " + mapType);
        };
    }

    @Benchmark
    public Map<String, Integer> put(Blackhole blackhole) {
        var map = createMap(); // Pay small price for instantiating the map.
        for (var i = 0; i < NUM_KEYS; i++) {
            var key = Integer.valueOf(i); // Box just once, not twice.
            blackhole.consume(map.put("key:"+key, key));
        }
        return map;
    }

    @Benchmark
    public Map<String, Integer> get(Blackhole blackhole) {
        for (var i = 0; i < NUM_KEYS; i++) {
            blackhole.consume(unchangingMap.get(i));
        }
        return unchangingMap;
    }

    @Benchmark
    public Map<String, Integer> remove(Blackhole blackhole) {
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
