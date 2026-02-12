# Public API

This document describes the public surface of LexCorpusEngine.
Internal classes and methods are intentionally omitted.

---

## LCEngine

### Constructors

```java
LCEngine()
LCEngine(SearchConfig config)
````

Creates a new in-memory search engine instance.

* The no-argument constructor initializes the engine with **default configuration**
* The overloaded constructor allows full customization via `SearchConfig`

---

### Ingestion

```java
void ingest(int docId, String text)
```

Ingests a document into the engine.

* `docId` must be unique
* `text` is normalized, tokenized, and stopwords are removed internally
* Ingestion updates search indices and the autocomplete trie

---

### Search

```java
List<Integer> search(String query, int k)
List<Integer> search(String query)
```

Executes a BM25-ranked search.

* `query` is normalized internally
* `k` specifies the maximum number of results
* The overload without `k` uses a default value

Returns a list of document IDs ordered by relevance.

---

### Autocomplete

```java
List<String> autocomplete(String prefix)
```

Returns Top-K autocomplete suggestions for the given prefix.

* Suggestions are ranked by observed token frequency

---

### Deletion

```java
void deleteDocument(int docId)
```

Deletes a document from the engine.

* Search indices are updated eagerly
* Autocomplete uses a lazy deletion strategy with rebuild thresholds

---

### Persistence

```java
void save(Path path) throws IOException
static LCEngine load(Path path) throws IOException
```

Persists and restores engine state using a custom binary format.

* Only internal index state is persisted
* Derived structures are rebuilt on load

---

## Configuration

LexCorpusEngine is configurable through the `SearchConfig` class.
All parameters have sensible defaults, allowing zero-config usage.

---

### Creating a Custom Configuration

```java
SearchConfig config = SearchConfig.builder()
    .bm25K1(1.5)
    .bm25B(0.75)
    .cachePolicy(CachePolicy.LRU)
    .cacheSize(1000)
    .autocompleteTopK(5)
    .build();

LCEngine engine = new LCEngine(config);
```

---

### Configuration Parameters and Defaults

| Parameter          | Description                                 | Default              |
| ------------------ | ------------------------------------------- | -------------------- |
| `bm25K1`           | BM25 term frequency scaling factor          | `1.5`                |
| `bm25B`            | BM25 document length normalization factor   | `0.75`               |
| `cachePolicy`      | Query result cache policy                   | `LRU`                |
| `cacheSize`        | Maximum number of cached queries            | `1000`               |
| `autocompleteTopK` | Maximum suggestions per autocomplete prefix | `5`                  |
| `defaultIndexPath` | Default persistence path                    | `data/lce_index.lce` |

---

### Cache Policies

Supported cache policies:

* `NONE` – disables caching
* `LRU` – least-recently-used cache (default)

Caching is applied after query normalization.
Cache invalidation occurs automatically on document ingestion and deletion.

---

### Design Notes

* Defaults are chosen to balance performance and simplicity
* Configuration is immutable after engine construction
* Advanced caching strategies (e.g. ARC) are planned for future releases

---

## Stability Guarantees

Only the APIs documented here are considered stable.
