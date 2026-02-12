# Architecture Overview

LexCorpusEngine is an in-memory lexical search engine composed of three
well-isolated components, coordinated through a single umbrella API.

---

## Core Components

- **SearchEngine**  
  Maintains the forward index, inverted index, and BM25 ranking logic.

- **AutocompleteEngine**  
  Provides trie-based prefix suggestions with Top-K ranking.

- **LCEngine**  
  Owns lifecycle, ingestion, persistence, and cross-engine coordination.

This separation ensures that each subsystem remains focused while avoiding
shared mutable state across boundaries.

---

## High-Level Data Flow

```

Document
↓
Normalize → Tokenize → Stopword Removal
↓
Forward Index + Inverted Index + Trie
↓
Search (BM25) / Autocomplete
↓
Ranked Results

```

All data structures are maintained in memory and updated incrementally.

---

## Ingestion Pipeline

The ingestion pipeline is designed to be linear, cache-friendly, and predictable.

### Steps

1. Text normalization
2. Tokenization
3. Stopword filtering
4. Forward index update
5. Inverted index update
6. Autocomplete trie update

### Design Notes

- Normalization and tokenization are performed once per document
- Stopwords are removed early to reduce noise and index size
- HashMaps are aggressively pre-sized to minimize rehashing
- The autocomplete trie is built during ingestion to avoid rescanning the index

Time complexity is **O(total tokens ingested)**.

---

## Search & Ranking (BM25)

LexCorpusEngine uses the BM25 ranking function proposed by
**Stephen Robertson** and **Karen Sparck Jones**.

### BM25 Algorithm

BM25 improves upon TF-IDF by:
- normalizing for document length
- saturating term frequency growth
- reducing bias toward long documents

### Parameters

- `k1 = 1.5`
- `b = 0.75`

These values are widely used defaults in information retrieval systems.

### Scoring Optimization

- Only candidate documents are scored
- Score computation and Top-K heap construction are performed simultaneously
- No full score map is materialized

This reduces memory usage and improves cache locality.

---

## Autocomplete Engine

Autocomplete is implemented using a trie with Top-K suggestions stored per node.

### Design

- Each trie node maintains up to K most frequent tokens
- Trie is updated incrementally during ingestion
- No post-ingestion vocabulary scan is required

### Top-K Trade-off

- Membership checks are linear in K
- Since K is intentionally small, this is acceptable
- A future optimization could use a HashSet for O(1) membership checks

---

## Deletions

- Document deletions update the search indices eagerly
- Autocomplete uses a **lazy deletion** strategy
- A rebuild threshold prevents excessive staleness without incurring high overhead

---

## Threading Model

The current implementation is single-threaded.

Concurrency is intentionally out of scope for v1.0 to keep the engine
deterministic and easier to reason about.

---


