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
- [ ] Follow-ups keep context for at least 5 turns
- [ ] Nothing is persisted unless the user taps Continue in app or enabled the setting
- [ ] Uses the shared `LlmRepository` (f007): no second model load
