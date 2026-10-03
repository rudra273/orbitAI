# f027 Temporary (incognito) chats
Labels: enhancement, good first issue
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** S · **Area:** Chat / Privacy

**Title:** A chat mode that isn't saved and never writes to Memory

**Description:**
Every message is persisted to Room, and the regex memory extractor runs on every user message when Memory is on. For sensitive one-off questions, users want a guarantee that nothing is stored. It also suits an on-device, privacy-first assistant.

**Proposed behavior:**
- "New temporary chat" entry (ghost icon) on the home screen and in the chat top bar
- Messages are held in memory only, never written to `messages`/`chats`
- Memory extraction, auto-titles and the chat list skip temporary chats
- A banner at the top: "Temporary chat · not saved"
- Leaving the screen discards it (confirm when non-empty); Export (f026) still possible

**Acceptance Criteria:**
- [ ] Instrumented test: after a temporary chat, `chats`, `messages` and `memories` have no new rows
- [ ] RAG over Spaces still works in temporary chats
- [ ] Process death discards the chat (no persistence through `SavedStateHandle`)
