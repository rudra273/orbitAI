# f035 More knowledge sources: DOCX, EPUB, web pages, image OCR and in-app notes
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** L · **Area:** Spaces
**Depends on:** f034

**Title:** Broaden what can be added to a Space

**Description:**
Spaces accept PDF and text-like files (`isTextLikeDocument`). Common inputs fail: Word documents, e-books, a web article, a photo of notes or a scanned PDF, or a quick note typed by the user.

**Proposed behavior (each can ship separately):**
1. **DOCX:** unzip + parse `word/document.xml` (no heavy dependency)
2. **EPUB:** unzip + strip XHTML chapters
3. **Web page:** paste a URL → fetch (respecting the cleartext-off policy) → readability-style main-text extraction → store title + URL
4. **OCR:** images and scanned PDFs via an on-device text recognizer (decide on a dependency, e.g. bundled ML Kit Text Recognition, vs none)
5. **Notes:** create and edit a plain-text note inside a Space

**Acceptance Criteria:**
- [ ] Each source type has a unit test with a small fixture file
- [ ] Web fetch shows the URL being fetched and never runs without user action
- [ ] OCR runs fully on-device; if the chosen library needs Play services, that is stated in the PR
- [ ] Unsupported files show a specific message listing the supported types
