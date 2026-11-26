# f037 Replace noisy regex memory extraction with "Remember this?" suggestions
Labels: bug, enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Memory

**Title:** Auto-memory should save only real personal facts, with user confirmation

**Description:**
`ChatViewModel.extractMemoryFacts()` runs 11 regexes on every user message when Memory is on and saves matches **without asking**. They over-match:
- "I am a bit confused about this" → saves *"User is a bit confused about this"*
- "I prefer you answer in bullets" → saves *"User likes/loves you answer in bullets"*
- "I work on Android apps" → no match; "I work in a team of 5" → *"User works at a team of 5"*

These junk memories are then injected into **every** prompt (see f038), which degrades answers and wastes context.

**Proposed behavior:**
- Remove automatic regex saving
- Lightweight candidate detection (keep a few high-precision patterns: name, location, job, "remember that…"); optionally ask the loaded local model to classify the candidate
- Show a dismissible chip under the user message: **"Remember: Lives in Pune?"** [Save] [Edit] [✕]
- Settings → Memory: "Suggest memories" toggle; explicit `remember …` commands still save directly (existing `DeviceCommand.Remember`)
- Dedupe against existing memories (case-insensitive + similarity)

**Acceptance Criteria:**
- [x] Unit tests: a negative set of 30 everyday sentences produces zero suggestions; a positive set produces correct facts
- [x] Nothing saved without a tap (except explicit remember commands)
- [x] Temporary chats (f027) never suggest

**Status:** 🟡 Done, device check pending (2026-10-04). Regex auto-save removed. MemorySuggester recognises a few high-precision phrasings (name, location, job, birthday, age, favourite, diet/allergy, reply language) and skips questions, hypotheticals and facts about others; a 'Remember: …?' chip under the message offers Save / Edit / dismiss. Settings > Memory > Suggest memories toggle; explicit 'remember …' still saves directly; duplicates and temporary chats are skipped. 30-sentence negative set and 15 positives in MemorySuggesterTest.
