# f012 Stop writing to the database on every streamed token
Labels: bug, enhancement
**Milestone:** M1 Foundation · **Priority:** P0 · **Size:** M · **Area:** Performance

**Title:** Keep the streaming reply in memory and persist it in throttled batches

**Description:**
During generation, each token calls `chatRepo.updateMessage(...)`, which runs an `UPDATE messages` in Room. Room invalidates the `messages` table, so `chatDao.observeAllChatsWithMessages()` re-queries **every chat with every message**. `ChatRepository.chats` then remaps them all to domain objects and recomposes the UI. That costs O(total messages in the app) per token: the CPU it steals from inference shows up as lower tokens/sec, the UI janks, and battery drains.

**Proposed behavior:**
- Hold the in-flight assistant text in a `MutableStateFlow<StreamingMessage?>` that the chat screen overlays on the persisted list
- Persist at most every ~500 ms, and always once on completion, cancellation or error (keep the existing `NonCancellable` final write)
- The chat detail screen observes only its own chat's messages (see f013)

**Acceptance Criteria:**
- [x] ≤ 2 DB writes per second while streaming
- [x] No lost text when the user stops generation, leaves the screen or the process dies mid-stream (worst case: the last ~500 ms)
- [ ] Measured tokens/sec on one LiteRT-LM model is equal or better than before (numbers in the PR)
- [x] No visible flicker or jump when the final persisted message replaces the streaming overlay

**Status:** 🟡 Done, pending device check (2026-10-03). `ChatRepository.updateMessage` keeps streaming text in an in-memory overlay (`streamingContent`) merged into `chats`, and writes to Room at most every 500 ms per message plus a final write on completion/stop/error (existing `NonCancellable` path). No ViewModel changes needed. The chat list still re-maps in memory per token; removing that is f013. Streaming confirmed working on device by owner. ⚠️ Tokens/sec comparison not yet measured.
