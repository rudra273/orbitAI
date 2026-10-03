# f025 Pin, rename and archive chats, plus full-text search across all messages
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** L · **Area:** Chat / Data
**Depends on:** f005, f013

**Title:** Chat organisation and real message search with Room FTS

**Description:**
`HomeScreen` search filters in memory on the chat title and the preview only (`displayTitle(it)` / `previewText(it)`), so a fact mentioned mid-conversation can't be found. Chats can't be renamed (the title is always the first 40 chars of the first message), pinned or archived. The only bulk action is "Delete all".

**Proposed behavior:**
- Swipe or long-press on a chat: **Pin**, **Rename**, **Archive**, **Delete**
- Pinned section at the top; Archived reachable from the list menu
- Full-text search over message content via a Room `@Fts4` table (`messages_fts`, content-synced to `messages`), with results showing the matching snippet and jumping to that message in the chat
- Multi-select delete/archive

**Acceptance Criteria:**
- [ ] Migration adds `isPinned`, `isArchived`, `updatedAt` to `chats` and creates the FTS table + triggers; migration test added (f005)
- [ ] Search across 10k messages returns in < 200 ms on a mid-range device
- [ ] Tapping a search hit scrolls to and highlights the message
- [ ] Rename persists and disables auto-titling (f030) for that chat
