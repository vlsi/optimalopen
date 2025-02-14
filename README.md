# OptimalOpenHashMap (in progress)

**OptimalOpenHashMap** is a Java implementation of a hash map using open addressing with linear probing. This implementation is inspired by the paper "[Optimal Bounds for Open Addressing Without Reordering](https://arxiv.org/abs/2501.02305)" by Martín Farach-Colton, Andrew Krapivin, and William Kuszmaul (2025).

## Features

- **High Load Factor**: Could/should work with a load factor up to 0.95, having good memory utilization.
- **Lazy Deletion**: Employs tombstones for deletions, with periodic cleanup when tombstones exceed a defined threshold.
- **Performance**: Demonstrates competitive performance compared to Java's standard `HashMap`.

## Status and warning

This is just an **example** implementation, it is not fully tested and will probably fail on edge cases.

Don't use in production code.

I'm not following the paper to every detail, I'm trying to keep the code performant as well. There are quite a few tweaks to try and pieces of code to optimize left.

## Usage

Take `OptimalOpenHashMap.java` and add it to your project.

`OptimalOpenHashMap` implements `Map<K, V>` so it could be used as a drop-in replacement for most current HashMap's.

### Performance

Again, no rigurous performance testing has been done, apart from a very simple comparison with inserts/retrieval/deletions.

```
Starting performance tests...
Warming up HashMap...
Insertion time: 3401 ms
Retrieval time: 911 ms
Deletion time: 666 ms
HashMap full benchmark total time: 4978 ms
---------------------------------------
Warming up OptimalOpenHashMap...
Insertion time: 3255 ms
Retrieval time: 969 ms
Deletion time: 748 ms
OptimalOpenHashMap full benchmark total time: 4972 ms
```

(a simpler linear version of this code was even ~20% faster)

These results indicate that `OptimalOpenHashMap` might be a viable option, showing faster retrieval and deletion times, with much better memory utilization.

Remember that HashMap has been tweaked and optimized over years and years, this is barely working.

### Contributing

If you're interested in hash table implementations or have insights on optimizing open addressing strategies, your contributions to this project would be greatly appreciated. Feel free to fork the repository, experiment with the code, and submit pull requests with improvements or new features.

I've played around with alternative deletion algorithms, including a nifty base-2 backward‑shift deletion, but that made the performance much worse.

### License

This project is licensed under the MIT License. See the LICENSE file for details.
