# f033 Show sources: citations from Spaces in answers
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** M · **Area:** Spaces / Chat UI
**Depends on:** f032

**Title:** Show which documents and pages an answer used, and let users open them

**Description:**
When Spaces are active, chunks are silently injected as `[Document N]`. Users can't tell whether an answer came from their files or from the model's own knowledge, which is the main trust problem with RAG. PDF chunks already carry `[Page N]` markers from `extractPdfPages()`.

**Proposed behavior:**
- Persist the retrieved chunk IDs on the assistant message (new `message_sources` table: messageId, chunkId, rank)
- Render a "Sources" row under the answer: chips like `Q3-report.pdf · p.12`
- Tapping a chip opens a bottom sheet with the chunk text highlighted, plus "Open document" (via the stored URI, if the permission still holds)
- Ask the model to cite `[1]`, `[2]` inline. Render the markers as tappable superscripts when present; the sources row is the reliable fallback

**Acceptance Criteria:**
- [x] Sources survive app restart (migration + test)
- [x] Deleting a document shows the source as "Removed document" instead of crashing
- [x] No Sources row when no chunk passed the relevance threshold (f032)

**Status:** 🟡 Done, device check pending (2026-10-04). DB v18 (MIGRATION_17_18): message_sources (messageId, rank, chunkId, docId, docName, page, excerpt; cascades with the message). The chunks actually sent after the budget become numbered sources on the reply; the prompt asks the model to cite [1], [2]. Answers show a Sources row (e.g. '2 · Q3-report.pdf · p.12'); tapping opens a sheet with the excerpt and Open document. Sources keep a snapshot, so a deleted document shows as 'Removed document'. Not done: inline [1] markers are plain text, not tappable superscripts. Device check pending: instrumented MigrationTest and the UI.
