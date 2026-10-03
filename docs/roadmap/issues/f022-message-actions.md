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
- [x] `MessageDao` gains `deleteMessagesAfter(chatId, timestampMs)` and `deleteMessage(id)`
- [x] Regenerate/edit use the same generation path as send (context budget, RAG, mode)
- [x] Actions disabled while a reply is streaming (except Stop)
- [x] Actions available to TalkBack via custom accessibility actions
- [x] Tool-only automation messages (email/WhatsApp/reminder drafts) don't offer Regenerate if it would re-fire an intent; they offer "Re-draft" instead

**Status:** ✅ Done (2026-10-03). AI replies get a small icon row under the text: Copy, Regenerate (latest reply only), Share, Report. This replaces the 'Report response' button that sat above every reply. User messages get a long-press menu: Copy, Edit & resend (dialog warns later replies are replaced), Delete (with confirmation); text selection on user bubbles gave way to this menu. Regenerate and edit keep the user message in place and generate with `resendExisting`, so cancelling the Gemini consent dialog never loses the message, and memory extraction isn't repeated. Device-command replies (call/remember/remind) show a note instead of regenerating; editing into a device command runs it normally. New DAO queries `deleteMessage` / `deleteMessagesFrom`. Copy/share use `markdownToPlainText` (keeps code blocks, strips emphasis; 4 unit tests). Edit/Delete/Regenerate are disabled while a reply is streaming and exposed as TalkBack custom actions. Icons verified on SM-S928B. Note: 'Read aloud' arrives with f024.
