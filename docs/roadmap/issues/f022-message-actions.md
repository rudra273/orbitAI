# f022 Message actions: copy, regenerate, edit & resend, delete, share
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P0 · **Size:** M · **Area:** Chat
**Depends on:** f008

**Title:** Long-press / action row on messages with the actions every chat app has

**Description:**
`MessageBubble` in `ChatScreen.kt` renders text only. The only copy affordance is inside fenced code blocks (`CodeBlockView`). Users can't copy a full answer, retry a bad one, fix a typo in their prompt, or remove a message, which is basic for an AI chat app.

**Proposed behavior:**
- Assistant message: **Copy**, **Regenerate**, **Share**, **Read aloud** (f024), **Report** (existing `ContentReportDialog`)
- User message: **Copy**, **Edit & resend**, **Delete**
- Regenerate replaces the last assistant reply. Edit & resend truncates history after the edited message (confirm dialog when later messages exist)
- Copy strips markdown syntax but keeps code
- Haptic feedback + snackbar confirmation

**Acceptance Criteria:**
- [ ] `MessageDao` gains `deleteMessagesAfter(chatId, timestampMs)` and `deleteMessage(id)`
- [ ] Regenerate/edit use the same generation path as send (context budget, RAG, mode)
- [ ] Actions disabled while a reply is streaming (except Stop)
- [ ] Actions available to TalkBack via custom accessibility actions
- [ ] Tool-only automation messages (email/WhatsApp/reminder drafts) don't offer Regenerate if it would re-fire an intent; they offer "Re-draft" instead
