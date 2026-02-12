
# LexCorpusEngine (LCE)

LexCorpusEngine is a lightweight, in-memory lexical search engine written in Java.
It provides BM25-based ranking, inverted indexing, trie-based autocomplete, caching,
and binary persistence with an emphasis on correctness, performance, and clarity of design.

The project is intentionally scoped as an **in-memory engine** suitable for
small-to-medium corpora, experimentation, and embedded use cases.

## Performace Scope

LexCorpusEngine is designed for interactive search on in-memory corpora.

On typical hardware, it can handle thousands of documents
with sub-millisecond to low-millisecond query latency.

Performance depends on dataset characteristics and environment.


---

## Features

- Forward and inverted index
- BM25 ranking
- Trie-based autocomplete with Top-K suggestions
- Stopword-aware ingestion pipeline
- Query result caching (LRU)
- Binary persistence (versioned, schema-driven)
- Zero-config defaults with configurable parameters
- No external runtime dependencies

---

## Quick Start

```java
LCEngine engine = new LCEngine();

engine.ingest(1, "Fast red car drives on the highway");
engine.ingest(2, "Electric car with fast charging");

engine.search("fast car", 5);
engine.autocomplete("ca");

engine.save(Path.of("data/index.lce"));

LCEngine loaded = LCEngine.load(Path.of("data/index.lce"));
````

Run the included demo:

```bash
mvn test
```

---

## Documentation

Detailed documentation is available under the `docs/` directory:

* [Architecture Overview](docs/architecture.md)
* [Persistence Format](docs/persistence.md)
* [Public API](docs/api.md)

---

## Design Philosophy

* **Algorithm-first**: correctness and asymptotic behavior over framework complexity
* **Explicit trade-offs**: optimizations are deliberate and documented
* **Future-proofing**: persistence is schema-based, not object-serialized
* **Minimalism**: avoid unnecessary dependencies and abstractions

---

## Notes on Algorithms & Optimizations

### BM25 Ranking

* Uses the BM25 ranking function proposed by Robertson and Sparck Jones.
* Parameters:

  * `k1 = 1.5`
  * `b = 0.75`
* These values are commonly used defaults in IR literature and systems such as Lucene.

### Stopword Handling

* Stopwords are removed during ingestion to reduce index size and noise.
* Stopword list sourced from:
  [https://gist.github.com/sebleier/554280](https://gist.github.com/sebleier/554280)

### Scoring Optimization

* Document scoring and Top-K heap construction are performed **simultaneously**
  to avoid storing full score maps.

### Ingestion Optimizations

* Trie for autocomplete is built incrementally during ingestion to avoid rescanning
  the vocabulary.
* Aggressive pre-sizing of HashMaps during ingestion reduces rehashing overhead.

### Autocomplete Top-K Optimization

* Each trie node maintains a Top-K list.
* Membership check is linear in K.
* Since K is very small, this is acceptable.
* A future optimization would use:

  * HashSet for O(1) membership
  * Array/List for ordering

---

## Limitations & Future Work

### Stateful Server-Side Autocomplete

The current autocomplete implementation traverses from the root on every call.
For very high query throughput, a stateful approach could reduce repeated traversal.
This is intentionally omitted in v1.0 to keep the library stateless and simple.

### Autocomplete Deletions

Autocomplete deletions are implemented using **lazy deletion** with a rebuild threshold
to prevent excessive staleness while keeping deletions fast.

### Caching

* Query-result caching uses an LRU cache for minimal overhead.
* ARC caching could improve hit ratio but adds complexity and metadata overhead.
* ARC is planned for a future version.


---

## Time Complexity

The following summarizes the asymptotic time complexity of core operations.
All complexities assume in-memory execution.

### Ingestion

**Single document ingestion**

* Normalization + tokenization: `O(T)`
* Forward index update: `O(T)`
* Inverted index update: `O(T)`
* Autocomplete trie update: `O(T · K)`

Where:

* `T` = number of tokens in the document
* `K` = autocomplete Top-K (small constant)

**Overall:** `O(T)`

---

### Search (BM25)

* Query normalization + tokenization: `O(Q)`
* Candidate document lookup: `O(Σ df(t))`
* BM25 scoring + Top-K heap construction: `O(C · log K)`

Where:

* `Q` = number of query tokens
* `df(t)` = document frequency of token `t`
* `C` = number of candidate documents

**Overall:** `O(Σ df(t) · log K)`

* In practice, `K` is small and scoring is performed only on candidate documents.
* Tokens are processed in order for most rare to most common , reducing internal load.
* Scoring and Top K is done Simultaneously to optimize for time and memory.


---

### Autocomplete

* Trie traversal: `O(P)`
* Top-K retrieval: `O(1)`

Where:

* `P` = prefix length

**Overall:** `O(P)`

---

### Deletion

* Search index deletion: `O(T)`
* Autocomplete update: `O(T)` (lazy)

Where:

* `T` = number of tokens in the deleted document

**Overall:** `O(T)`

---

### Persistence

**Save**

* Serializing indices: `O(N)`

**Load**

* Reading indices: `O(N)`
* Rebuilding derived structures: `O(N)`

Where:

* `N` = total number of indexed entries

---

### Caching

* Cache lookup: `O(1)`
* Cache insertion / eviction (LRU): `O(1)`

---

### Notes

* All operations are single-threaded.
* HashMap operations assume average-case `O(1)`.
* Constants are kept small via aggressive pre-sizing and incremental construction.
* No full index scans occur during queries.

---

## Versioning

Current version: **1.0**

Persistence format is versioned and backward-incompatible changes will be gated
by explicit version checks.

---

## License

MIT License
