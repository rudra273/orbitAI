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
- [x] RAG over Spaces still works in temporary chats
- [x] Process death discards the chat (no persistence through `SavedStateHandle`)

**Status:** 🟡 Done, pending device test run (2026-10-04). `ChatRepository` keeps temporary chats (IDs prefixed `temp-`) in an in-memory map; every chat operation (get, add/update/delete message, edit, model change, observe) has an in-memory branch, so generation, regenerate/edit and the chat screen work unchanged and nothing is written to Room. They never appear in the chat list. Memory auto-extraction is skipped for temporary chats; RAG over Spaces works as usual. UI: a ghost-icon 'New temporary chat' button above the + on the home screen, a 'Temporary chat · not saved' banner, and leaving a non-empty one (system or top-bar Back) asks 'Discard temporary chat?'. Discarding happens only on leave, not on rotation or when briefly opening Settings; process death clears it because it lives only in memory. Instrumented `TemporaryChatTest` checks no rows are added to chats/messages/memories. ⚠️ Run on device: `./gradlew installDebugAndroidTest` then `adb shell am instrument -w com.example.orbitai.test/androidx.test.runner.AndroidJUnitRunner`. Export (f026) not built yet.
