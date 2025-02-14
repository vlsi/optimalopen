# OptimalOpenHashMap (in progress)

## UPDATE:

I haven't released the implementatino yet, this is a simpler linear probing version.

**OptimalOpenHashMap** is a Java implementation of a hash map using open addressing with linear probing. This implementation is inspired by the paper "[Optimal Bounds for Open Addressing Without Reordering](https://arxiv.org/abs/2501.02305)" by Martín Farach-Colton, Andrew Krapivin, and William Kuszmaul (2025).

## Features

- **High Load Factor**: Allows a load factor up to 0.95 for efficient memory utilization.
- **Lazy Deletion**: Employs tombstones for deletions, with periodic cleanup when tombstones exceed a defined threshold.
- **Performance**: Demonstrates competitive performance compared to Java's standard `HashMap`.

## Warning

This is just an example implementation, it is not fully tested and will probably fail on edge cases.

Use this in your projects at your own risk.

## Usage

Take `OptimalOpenHashMap.java` and add it to your project.

`OptimalOpenHashMap` implements `Map<K, V>` so it could be used as a drop-in replacement for most current HashMap's.

### Performance

Again, no rigurous performance testing has been done, apart from a very simple comparison with inserts/retrieval/deletions.

```
Starting performance tests...
Warming up HashMap...
Insertion time: 3051 ms
Retrieval time: 1256 ms
Deletion time: 960 ms
HashMap full benchmark total time: 5267 ms
---------------------------------------
Warming up OptimalOpenHashMap...
Insertion time: 3018 ms
Retrieval time: 772 ms
Deletion time: 553 ms
OptimalOpenHashMap full benchmark total time: 4343 ms
```

These results indicate that `OptimalOpenHashMap` might be a viable option, showing faster retrieval and deletion times, with much better memory utilization (using a load factor of 90% instead of 75%).

### Contributing

If you're interested in hash table implementations or have insights on optimizing open addressing strategies, your contributions to this project would be greatly appreciated. Feel free to fork the repository, experiment with the code, and submit pull requests with improvements or new features.

I've played around with alternative deletion algorithms, including a nifty base-2 backward‑shift deletion, but that made the performance much worse.

### License

This project is licensed under the MIT License. See the LICENSE file for details.
