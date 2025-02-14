package com.royvanrijn.optimalopen;

import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapEntrySetTester;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Map;

public class MapTest {

    public static Test suite() {
        TestSuite suite = new TestSuite("All tests");
        suite.addTest(tests("OptimalOpenHashMap", new TestStringMapGenerator() {
            @Override
            protected Map<String, String> create(Map.Entry<String, String>[] entries) {
                return populate(new OptimalOpenHashMap<>(), entries);
            }
        }));
        suite.addTest(tests("LinearOpenHashMap", new TestStringMapGenerator() {
            @Override
            protected Map<String, String> create(Map.Entry<String, String>[] entries) {
                return populate(new LinearOpenHashMap<>(), entries);
            }
        }));
        return suite;
    }

    private static TestSuite tests(final String name, TestStringMapGenerator generator) {
        return MapTestSuiteBuilder
                .using(generator)
                .named(name)
                .withFeatures(
                        MapFeature.GENERAL_PURPOSE,
                        MapFeature.ALLOWS_ANY_NULL_QUERIES,
                        MapFeature.RESTRICTS_KEYS,
                        MapFeature.RESTRICTS_VALUES,
                        CollectionFeature.GENERAL_PURPOSE,
                        CollectionFeature.SERIALIZABLE,
                        CollectionSize.ANY)
                // Remove the suppression when entrySet() implementation improves
                .suppressing(Helpers.getMethod(MapEntrySetTester.class, "testEntrySetIteratorRemove"))
                .createTestSuite();
    }

    private static <T, M extends Map<T, String>> M populate(M map, Map.Entry<T, String>[] entries) {
        for (Map.Entry<T, String> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}
