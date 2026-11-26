# f026 Export and share a conversation
Labels: enhancement, good first issue
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Chat

**Title:** Export a chat as Markdown, plain text or PDF

**Description:**
There's no way to get a conversation out of OrbitAI: users can't save an answer to their notes, send a research thread to a colleague, or keep a record.

**Proposed behavior:**
- Chat top-bar menu → **Export** → Markdown (.md), Plain text (.txt), PDF
- PDF via `android.graphics.pdf.PdfDocument` (no new dependency), with title, date and model name
- Output via the share sheet (`ACTION_SEND`) or Save to device (SAF `CreateDocument`)
- Option to include or exclude images (vision chats)

**Acceptance Criteria:**
- [x] Markdown export round-trips code blocks and lists correctly
- [ ] PDF renders long chats across pages without cut-off lines
- [x] Export runs off the main thread, with progress for long chats
- [x] Temporary chats (f027) can be exported before they're discarded

**Status:** 🟡 Done, device check pending (2026-10-04). Chat ⋮ menu → Export chat: Markdown, text or PDF (PdfDocument, paged by line); Share via FileProvider or Save via the system picker; images optional in PDF. Device check pending: long PDF paging and the share sheet.
