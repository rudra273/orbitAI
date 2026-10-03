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
- [ ] A scanned PDF is reported as such, not as "Done · 0 chunks"
- [ ] Ingestion runs at low priority and pauses while a chat reply is generating
