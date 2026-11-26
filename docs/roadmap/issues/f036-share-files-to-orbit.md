# f036 Share files and images to Orbit from any app
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** System integration / Spaces

**Title:** Accept PDFs, images and documents from the share sheet: "Ask Orbit" or "Add to Space"

**Description:**
`ProcessTextActivity` registers `ACTION_SEND` for `text/plain` only. Sharing a PDF from Files or Gmail, or a photo from Gallery, doesn't offer Orbit. That's the most natural way to "chat with this document" or "explain this screenshot".

**Proposed behavior:**
- Register `ACTION_SEND` / `ACTION_SEND_MULTIPLE` for `application/pdf`, `image/*` and supported text MIME types on a new `ShareReceiverActivity`
- Chooser sheet: **Ask Orbit about this** (opens a new chat with the attachment) · **Add to Space…** (pick or create a Space, then ingest via f034)
- For images, "Ask Orbit" picks a vision-capable model or explains that one is needed
- Text sharing keeps today's "Create reminder" / "Ask Orbit" options

**Acceptance Criteria:**
- [x] Persistable URI permission taken when possible; otherwise the file is copied to app storage
- [ ] Sharing 5 PDFs to a Space ingests all of them
- [ ] Works from Files, Gmail attachments and Google Photos

**Status:** 🟡 Done, device check pending (2026-10-04). ShareReceiverActivity takes PDFs, images and text files (single or multiple): Ask Orbit opens a new chat with them attached and switches to a model that can read them; Add to Space picks or creates a Space and indexes the documents. Shared links are always copied to app storage (files/shared) because share grants are temporary. Device check pending: Files, Gmail, Google Photos, 5 PDFs to a Space.
