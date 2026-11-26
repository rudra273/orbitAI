# f031 Upgrade the on-device embedding model and re-index Spaces
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** L · **Area:** Spaces / RAG

**Status:** 🟡 Built, device check pending (2026-10-04). Owner asked for an ungated model, so this uses multilingual-e5-small (MIT, ungated, Hindi included) instead of EmbeddingGemma: Settings > Models > Multilingual E5 Small > Download, then Use. Vectors record their model (DB v21); switching re-indexes Spaces and Memory in the background and unconverted items are found by keywords meanwhile. The old Universal Sentence Encoder stays as the small option. Not done: the 20-question recall@5 comparison (needs sample documents), display rename of `gecko-embedding`.

**Title:** Replace Universal Sentence Encoder with a modern multilingual embedder, with a re-index migration

**Description:**
RAG uses the Universal Sentence Encoder `.tflite` (6 MB) through MediaPipe `TextEmbedder`. It's small, but old, English-centric and weak at matching question → passage, which caps answer quality in Spaces. The config ID `gecko-embedding` is misleading because the model is USE, not Gecko.

Newer on-device embedding models (e.g. Google's EmbeddingGemma family, if a LiteRT build fits mobile RAM) retrieve far better and handle Hindi and other languages.

Per project rules, the **owner supplies the verified model URL and revision**. Nothing is guessed.

**Proposed behavior:**
- Add the new embedder as a second `EmbeddingModelConfig`; keep USE as a "Lite" option for low-RAM devices
- Store `embeddingModelId` + vector dimension on each `rag_chunks` row (migration)
- On switching embedders, a background job (f034) re-embeds existing chunks with progress; old vectors are ignored until done
- Retrieval refuses to compare vectors from different models

**Acceptance Criteria:**
- [ ] Owner-provided URL recorded in `docs/model-catalog-audit.md`
- [ ] Small eval set (20 questions over 3 sample docs) in `app/src/test/resources`; recall@5 reported for old vs new embedder in the PR
- [ ] Re-indexing is resumable and doesn't block chat
- [ ] Rename the `gecko-embedding` ID for display only (keep the stored ID for compatibility)
