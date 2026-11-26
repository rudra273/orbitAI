# f049 Bubble: follow-up questions and "Continue in app"
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M · **Area:** Bubble
**Depends on:** #39, f007

**Title:** Make bubble answers conversational and keep them in history

**Description:**
The bubble answers one question, then the result card is dismissed. Its exchanges aren't saved anywhere, so users can't ask "and what about tomorrow?" or find the answer later.

**Proposed behavior:**
- Result card gets a **Reply** mic/text button: follow-ups go to the same `ConversationSession` (already multi-turn capable)
- **Continue in app:** saves the bubble exchange as a chat (tagged "From Orbit Bubble") and opens it
- Setting: "Save bubble conversations to history" (default off, privacy-first)
- Screen-capture answers keep the screenshot only if the user taps Continue in app

**Acceptance Criteria:**
- [x] Follow-ups keep context for at least 5 turns
- [x] Nothing is persisted unless the user taps Continue in app or enabled the setting
- [x] Uses the shared `LlmRepository` (f007): no second model load

**Status:** 🟡 Done, device check pending (2026-10-04). Reply button (voice) on the bubble card continues the same session: last 8 exchanges are passed back to the model (conversation history, or written into the prompt for non-LiteRT models), so the model keeps context. Continue in app saves the exchange as a chat titled 'From Orbit Bubble: ...' and opens it. New setting 'Save bubble conversations to history' (off by default). Only text is saved; screenshots are never stored. Not done: a typed follow-up box (voice only). Each follow-up replaces the card text; the earlier turns are visible in the saved chat.
