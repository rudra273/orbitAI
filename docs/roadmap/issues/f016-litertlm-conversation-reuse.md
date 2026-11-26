# f016 Reuse LiteRT-LM conversations across turns in the main chat
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** L · **Area:** Engine / Performance
**Depends on:** f011, f014

**Title:** Keep a live `ConversationSession` per open chat so each turn only prefills the new message

**Description:**
`LiteRtLmEngine.generateResponseStream()` creates a new `Conversation` for every call and receives the *entire* re-rendered history as one text message. Every turn re-prefills the whole chat, so time-to-first-token grows linearly with chat length. The bubble already uses `createConversation()` + `streamTurn()` (multi-turn, tool-capable). The main chat does not.

**Proposed behavior:**
- When the active model supports conversations (`llmRepo.supportsConversations()`), the chat keeps one `ConversationSession` per open chat with the mode system prompt as `systemInstruction`
- Subsequent turns send only the new user message (+ RAG context for that turn)
- The session is rebuilt from history when the user switches chat, model or mode, edits/regenerates a message (f022), or the context budget (f011) requires dropping turns
- Non-LiteRT engines keep the stateless path

**Acceptance Criteria:**
- [ ] Time-to-first-token on turn 10 of a chat is within ~20% of turn 1 (numbers in the PR)
- [x] Switching chats or modes never leaks one chat's history into another
- [x] Session is closed on chat exit, model switch and `onCleared()`
- [x] Works with image input on vision models

**Status:** 🟡 Done, device check pending (2026-10-04). LiteRT chats keep one live ConversationSession (keyed by chat, model, settings and system prompt) and send only the new message plus that turn's Spaces excerpts and memories. ConversationReuse rebuilds from history after an edit, regenerate, deleted message, trimmed history, a tool call, a stopped/failed turn, or when the session nears 75% of the context window (unit-tested). Closed on chat exit, app background (so the bubble never overlaps), model unload (LlmRepository close listener) and onCleared; a session mid-turn closes when the turn ends. Images go into the turn as ImageBytes. Device check pending: time-to-first-token on turn 1 vs turn 10.
