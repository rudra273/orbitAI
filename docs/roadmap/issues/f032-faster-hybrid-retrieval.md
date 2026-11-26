# f032 Faster and better retrieval: cached embedder, in-memory index, hybrid search
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Spaces / Performance

**Title:** Stop reloading the embedder on every query and combine keyword and semantic ranking

**Description:**
`SpaceRepository.searchChunksInSpaces()` calls `EmbeddingModel.create(context)` (loads the TFLite model from disk) **for every chat message** and closes it right after. It then reads every chunk BLOB in the active spaces from SQLite, deserialises each, and runs brute-force cosine similarity. `processDocument()` also creates its own embedder per document. Keyword search is the fallback only when embeddings are missing, and it uses `LIKE '%kw%'` per keyword.

**Proposed behavior:**
- One cached `EmbeddingModel` instance owned by the app container (f007), released on `onTrimMemory`
- In-memory float matrix per active Space set, built on first query and invalidated on document add/delete
- **Hybrid ranking:** FTS (BM25-like, reusing the f025 FTS approach for `rag_chunks`) + cosine, merged with reciprocal-rank fusion
- Drop near-duplicate chunks (overlap is 125 words) before injecting into the prompt
- Similarity threshold: if nothing is relevant, inject nothing and don't let the model claim document support

**Acceptance Criteria:**
- [ ] Query latency over 5,000 chunks < 150 ms after warm-up (numbers in the PR)
- [x] Exact-term queries (names, IDs, error codes) find the right chunk even when semantic similarity is low
- [x] No prompt injection of irrelevant chunks for small-talk messages
- [x] Unit tests for rank fusion and dedupe

**Status:** 🟡 Done, device check pending (2026-10-04). SharedEmbedder (one model, released on onTrimMemory) used by search and indexing; RetrievalIndex keeps the active Spaces' chunks in memory until documents change, ranks with BM25 (in memory instead of SQLite FTS, so no migration) plus cosine, merges by reciprocal-rank fusion, drops near-duplicates and returns nothing below the relevance cutoff (cosine 0.3 or no keyword match). 5,000 chunks: 1.8 ms per query on the JVM; device timing pending.
