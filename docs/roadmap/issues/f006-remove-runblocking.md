# f006 Remove `runBlocking` from `createNewChat` (main-thread database I/O)
Labels: bug, good first issue
**Milestone:** M1 Foundation · **Priority:** P1 · **Size:** S · **Area:** Chat

**Title:** Make chat creation asynchronous to avoid main-thread blocking and ANR risk

**Description:**
`ChatViewModel.createNewChat()` calls `kotlinx.coroutines.runBlocking(Dispatchers.IO) { ... }` so navigation can get the ID synchronously. That blocks the UI thread on Room I/O. It is usually fast, but it can stall on a busy disk or a DB still being migrated.

**Proposed behavior:**
Generate the chat UUID up front, insert it in `viewModelScope`, and navigate immediately. The chat screen already observes the chat by ID, so it fills in once the row exists. Alternatively, expose a suspend function and navigate from a `LaunchedEffect`/event.

**Acceptance Criteria:**
- [x] No `runBlocking` remains in `viewmodel/` or `ui/`
- [x] The "reuse an empty chat" behaviour (`findReusableEmptyChatId`) is preserved
- [x] Tapping "New chat" repeatedly never creates duplicate empty chats
- [x] StrictMode (see f010) reports no disk-read violations when creating a chat

**Status:** ✅ Done (2026-10-03). `createNewChat()` is now a `suspend` function serialized by a `Mutex` (rapid taps reuse the same empty chat). Call sites made async: HomeScreen via `openNewChat(onOpenChat)`, MainActivity overlay/share intent via `lifecycleScope`, NavGraph initial chat via `produceState` (keyed only on the onboarding flags, so consuming an overlay prompt can't create an extra chat). No `runBlocking` remains in `app/src/main`. StrictMode (f010) shows no violations from chat creation on device (2026-10-04).
