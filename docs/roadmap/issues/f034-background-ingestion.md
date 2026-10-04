# f034 Reliable background document ingestion with progress and error reasons
Labels: enhancement, bug
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Spaces

**Title:** Move document processing to WorkManager and tell users why a document failed

**Description:**
`SpaceRepository.addDocumentToSpace()` launches on a repository-private `CoroutineScope`, so a large PDF is lost if the process dies mid-ingest (it stays `PROCESSING` forever). `processDocument()` catches every exception and sets `ERROR` with **no reason**. `extractPdfPages()` also swallows errors and returns an empty list, so a scanned (image-only) PDF "succeeds" with 0 chunks.

**Proposed behavior:**
- `DocumentIngestWorker` (WorkManager) per document, idempotent, resumable after process death
- Stale `PROCESSING` rows are re-queued on app start
- Store an `errorMessage` on `rag_documents` (migration): "Password-protected PDF", "No selectable text (scanned PDF), try OCR (f035)", "File no longer accessible", "Out of memory"
- Per-document progress (pages processed / total) in `SpaceDetailScreen`
- Retry button on failed documents

**Acceptance Criteria:**
- [ ] Killing the app during ingest of a 300-page PDF resumes and completes on next start
- [x] A scanned PDF is reported as such, not as "Done · 0 chunks"
- [x] Ingestion runs at low priority and pauses while a chat reply is generating

**Status:** 🟡 Done, device check pending (2026-10-04). DocumentIngestWorker (WorkManager, unique per document, idempotent) replaces the repository scope; unfinished rows are re-queued on start. DB v15 adds errorMessage/pagesDone/pagesTotal (MIGRATION_14_15, MigrationTest extended). Rows show page progress, the failure reason and a Retry button; scanned PDFs report 'No selectable text'. Indexing waits while a chat/bubble run holds the model. Device check pending: kill during a 300-page PDF, scanned PDF, instrumented MigrationTest.
