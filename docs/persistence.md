# Persistence

LexCorpusEngine implements persistence using a custom binary format.
The persistence layer is designed to be explicit, deterministic, and
robust to future refactoring.

---

## Design Goals

- **Versioned**  
  Each persisted index includes a version header.

- **Deterministic**  
  The binary layout is fixed and schema-driven.

- **Refactor-safe**  
  Persistence is decoupled from Java object layouts.

- **No Java Serialization**  
  Avoids fragile, implementation-dependent formats.

---

## What Is Persisted

The following internal state is persisted:

- Total token count
- Average document length
- Forward index (document → token → frequency)
- Inverted index (token → document → frequency)

These structures are sufficient to fully reconstruct
search behavior.

---

## What Is Not Persisted

The following are **intentionally not stored**:

- Autocomplete trie
- Caches
- Derived scoring structures

These are rebuilt on load.

---

## Only Internal State Peristance

Persisting internal state rather than entire objects provides:

- Stability across refactors
- Explicit control over file format evolution
- Clear version boundaries
- Predictable startup behavior

Serializing objects directly would not improve load time
and would significantly reduce flexibility.

---

## Binary Format Overview

The persisted file follows this high-level layout:

1. Magic header
2. Version number
3. Global metadata
4. Forward index
5. Inverted index

All reads mirror writes.

---

## Versioning Strategy

- Each file includes a version identifier
- Incompatible versions are rejected explicitly
- Future versions will introduce backward-compatible loaders

---

## Startup Characteristics

Startup time is proportional to the number of indexed entries.
This is acceptable for the intended in-memory scope of the engine
and avoids the complexity of partial or lazy loading.

---

## Summary

The persistence layer favors:

- explicit schema over implicit serialization
- long-term maintainability
- Java objects are not stored as is in favor of flexibility


---
