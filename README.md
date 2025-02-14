# OptimalOpenHashMap (in progress)

**OptimalOpenHashMap** is a Java implementation of a hash map using open addressing with linear probing. This implementation is inspired by the paper "[Optimal Bounds for Open Addressing Without Reordering](https://arxiv.org/abs/2501.02305)" by Martín Farach-Colton, Andrew Krapivin, and William Kuszmaul (2025).

## Features

- **High Load Factor**: Could/should work with a load factor up to >0.9, having good memory utilization.
- **Lazy Deletion**: Employs tombstones for deletions, with periodic cleanup when tombstones exceed a defined threshold.
- **Performance**: Demonstrates competitive performance compared to Java's standard `HashMap`, slightly worse in the JMH benchmark.

## Status and warning

This is just an **example** implementation, it is not fully tested and will probably fail on edge cases.

Don't use in production code.

I'm not following the paper to every detail, I'm trying to keep the code performant as well. There are quite a few tweaks to try and pieces of code to optimize left.

## Usage

`OptimalOpenHashMap` implements `Map<K, V>` so it could be used as a drop-in replacement for most current HashMap's, but: **Don't**. Just don't use it.

### Performance

Again, no rigorous performance testing has been done, apart from a very simple comparison with inserts/retrieval/deletions.

```
Benchmark            (mapType)   Mode  Cnt    Score     Error  Units

JMHBenchmark.get           JDK  thrpt    3  184,710 ± 262,495  ops/s
JMHBenchmark.get       OPTIMAL  thrpt    3  126,351 ±  22,330  ops/s
JMHBenchmark.get        LINEAR  thrpt    3   60,854 ±  26,275  ops/s

JMHBenchmark.put           JDK  thrpt    3   16,162 ±   6,293  ops/s
JMHBenchmark.put       OPTIMAL  thrpt    3   13,890 ±   2,262  ops/s
JMHBenchmark.put        LINEAR  thrpt    3    4,883 ±   0,382  ops/s

JMHBenchmark.remove        JDK  thrpt    3   16,452 ±   7,423  ops/s
JMHBenchmark.remove    OPTIMAL  thrpt    3   13,950 ±   3,428  ops/s
JMHBenchmark.remove     LINEAR  thrpt    3    4,987 ±   0,303  ops/s
```

These results indicate that `OptimalOpenHashMap` might be a viable option (compared to my simple linear implementation), however currently it is still slower across the board.

Remember though that HashMap has been tweaked and optimized over years and years, this is barely working.

### Contributing

If you're interested in hash table implementations or have insights on optimizing open addressing strategies, your contributions to this project would be greatly appreciated. Feel free to fork the repository, experiment with the code, and submit pull requests with improvements or new features.

I've played around with alternative deletion algorithms, including a nifty base-2 backward‑shift deletion, but that made the performance much worse.

### License

This project is licensed under the MIT License. See the LICENSE file for details.
