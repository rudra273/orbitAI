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
- [ ] Snapshot/unit tests for each block type, plus a mid-stream incomplete-table case
- [ ] Links never auto-open; tapping shows the full URL first
- [ ] No jank while streaming long answers (renderer memoized per finished block)
- [ ] Bubble uses the same parser
