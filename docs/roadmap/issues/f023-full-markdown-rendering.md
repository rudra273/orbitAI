# f023 Complete markdown rendering: tables, links, quotes, numbered lists, syntax highlighting
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Chat UI

**Title:** Replace the minimal custom markdown parser with full CommonMark + GFM-table rendering

**Description:**
`parseMarkdownBlocks()` in `ChatScreen.kt` handles headings, bullets, paragraphs and fenced code. Ordered lists become bullets (numbers lost). Tables, block quotes, links, horizontal rules, nested lists and task lists render as raw text. Models produce these often, especially Gemini and Phi-4. The bubble has a separate `parseSimpleMarkdown()` with different behaviour.

**Proposed behavior:**
- One shared markdown renderer used by chat and bubble (the bubble can render a Spanned via the same AST)
- Support: ordered lists with numbering, nested lists, tables (horizontally scrollable), block quotes, links (open with confirmation), inline code, strikethrough, horizontal rules
- Code blocks: basic syntax highlighting for common languages + existing Copy button
- Streaming-safe: half-received fences or tables render gracefully mid-stream

**Decision:** library (e.g. a CommonMark parser) vs extending the in-house parser. Pick one in the PR, weighing APK size.

**Acceptance Criteria:**
- [x] Snapshot/unit tests for each block type, plus a mid-stream incomplete-table case
- [x] Links never auto-open; tapping shows the full URL first
- [ ] No jank while streaming long answers (renderer memoized per finished block)
- [x] Bubble uses the same parser

**Status:** 🟡 Done, pending visual check (2026-10-04). Decision: extend the in-house parser (no new dependency or APK cost). New pure `feature/chat/markdown/MarkdownParser` handles headings, paragraphs, ordered lists that keep their numbers, nested and task lists, block quotes, horizontal rules, GFM tables with alignment and ragged rows, and open/closed code fences. Inline: bold (can contain italic), italic, strikethrough, inline code, [links](url) and bare URLs; arithmetic and snake_case are not treated as emphasis. New `MarkdownView.kt` renders this in Compose: tables scroll horizontally, and links show the full URL in a confirmation dialog before opening. `CodeHighlighter` colours keywords, strings, comments and numbers for ~15 languages. Streaming: a table header without its separator stays text and an unclosed fence renders as an open code block. The bubble uses the same parser via `MarkdownHtml.toHtml`; it also now keeps the raw markdown while streaming (it previously re-parsed already-rendered text, losing markers mid-answer). 14 unit tests. Installed on SM-S928B; ⚠️ visual check of a table, list and code reply still needed.
