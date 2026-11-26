# f035 More knowledge sources: DOCX, EPUB, web pages, and in-app notes
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** L · **Area:** Spaces
**Depends on:** f034

**Status:** 🟡 Done except web pages (2026-10-04). Word (.docx) and EPUB files are indexed, and Spaces have in-app notes (Note button: create, tap to edit, saved as text and re-indexed). OCR was dropped and web pages are skipped on purpose (privacy: no new network path). Known limit: a restored backup brings back note text for search but the note file itself is not in the backup, so editing a restored note is unavailable.

**Title:** Broaden what can be added to a Space

**Description:**
Spaces accept PDF and text-like files (`isTextLikeDocument`). Common inputs fail: Word documents, e-books, a web article, a photo of notes or a scanned PDF, or a quick note typed by the user.

**Proposed behavior (each can ship separately):**
1. **DOCX:** unzip + parse `word/document.xml` (no heavy dependency)
2. **EPUB:** unzip + strip XHTML chapters
3. **Web page:** paste a URL → fetch (respecting the cleartext-off policy) → readability-style main-text extraction → store title + URL
4. **Notes:** create and edit a plain-text note inside a Space

**Acceptance Criteria:**
- [ ] Each source type has a unit test with a small fixture file
- [ ] Web fetch shows the URL being fetched and never runs without user action
- [ ] Unsupported files show a specific message listing the supported types
