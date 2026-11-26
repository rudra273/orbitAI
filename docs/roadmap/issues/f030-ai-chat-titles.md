# f030 Generate short chat titles with the model
Labels: enhancement, good first issue
**Milestone:** M2 Core Experience · **Priority:** P2 · **Size:** S · **Area:** Chat

**Title:** Replace "first 40 characters" titles with a 3–6 word generated title

**Description:**
`ChatRepository.addMessage()` sets the title to the first 40 characters of the first user message, so the list fills with titles like "can you help me with this thing i was…".

**Proposed behavior:**
- After the first assistant reply completes, run a tiny instruction prompt on the already-loaded local model: "Title this conversation in 3–6 words"
- Max ~16 output tokens, run only when idle, never on Gemini without consent
- Keep the 40-char fallback when the model is busy, fails, or the user disabled it (Settings → Chat → "Smart titles")
- Never overwrite a user-renamed title (f025)

**Acceptance Criteria:**
- [x] Title generation doesn't delay or block the next user message
- [x] Output sanitised (no quotes, no trailing punctuation, ≤ 48 chars)
- [x] Unit test for the sanitiser

**Status:** 🟡 Done, device check pending (2026-10-04). After a chat's first reply the loaded local model writes a 3-6 word title (16 tokens max, 20 s timeout) in its own job that any new message cancels; never on Gemini or temporary chats, never over a renamed chat (f025). ChatTitles.sanitize strips labels, quotes, markdown and trailing punctuation and caps at 48 chars (tested). Settings > Chat > Smart titles.
