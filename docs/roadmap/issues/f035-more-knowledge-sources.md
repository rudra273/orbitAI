# f035 More knowledge sources: DOCX, EPUB, web pages, and in-app notes
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** L · **Area:** Spaces
**Depends on:** f034

**Status:** 🚧 In progress (2026-10-04). Word (.docx) and e-book (.epub) files are now indexed in Spaces, with unit tests. Still to do: web page by URL, in-app notes.

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
