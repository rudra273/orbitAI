# f013 Load chat summaries for the list and one chat's messages for the detail screen
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Performance / Data

**Title:** Replace `observeAllChatsWithMessages()` with per-screen queries

**Description:**
`ChatRepository.chats` is built on `observeAllChatsWithMessages()`, which loads every message of every chat into memory, including image URI lists. The home list needs only title, last-message preview and timestamp. The chat screen needs only one chat. Startup time and memory grow with total history.

**Proposed behavior:**
- `ChatDao.observeChatSummaries()`: a SQL query returning `id, title, modelId, createdAt, lastMessagePreview, lastMessageAt, messageCount`
- `MessageDao.observeMessages(chatId)`: a `Flow<List<MessageEntity>>` for the chat screen
- Add an index on `messages(chatId, timestampMs)` (migration + schema JSON, see f005)
- Optional: Paging 3 for chats with 500+ messages

**Acceptance Criteria:**
- [x] HomeScreen search and date grouping still work on summaries
- [x] Opening a chat loads only that chat's messages
- [ ] Cold start with 200 chats × 50 messages is measurably faster (numbers in the PR)
- [x] Migration test added

**Status:** ✅ Done (2026-10-03). Home list now uses `ChatDao.observeChatSummaries()` (chat fields + newest-message preview and time via subqueries; `ChatSummary` domain type). The chat screen uses `ChatRepository.observeChat(id)` (that chat's messages only, with the f012 streaming overlay). The heavy `observeAllChatsWithMessages` query is removed. DB v14 adds index `messages(chatId, timestampMs)` via `MIGRATION_13_14` (schema 14.json committed); `ReminderPersistenceTest` now uses `ALL_MIGRATIONS` instead of a hard-coded list. On SM-S928B: 11/11 device tests pass, the real DB upgraded to v14, and the list (date groups and previews) and an opened chat render correctly. Paging was not needed. ⚠️ Cold-start timing not measured; do it with the f060 benchmarks.
