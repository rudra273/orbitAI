# f038 Inject only relevant memories, and let users edit them
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Memory / Prompt
**Depends on:** f011, f032

**Title:** Retrieve top-K memories per message instead of sending all of them every time

**Description:**
`memoryRepo.getAllMemories()` is concatenated into every prompt as `[Memory 1..N]`. With 50+ memories that's hundreds of tokens per turn on small-context models, most of them irrelevant ("bike parked on level 2" while asking about Python). `MemoryScreen` supports add and delete but not **edit**.

**Proposed behavior:**
- Embed memories with the shared embedder (f032); store vectors (migration)
- Per message: always include "profile" memories (name, language preference), plus the top-K (default 5) relevant ones above a threshold
- Memory screen: edit in place, search, sort by recent or most used, show source (auto/manual/profile)
- "Memory used" indicator on the assistant message (tap to see which memories)

**Acceptance Criteria:**
- [ ] With 200 memories, prompt memory section ≤ ~150 tokens
- [ ] Editing a memory re-embeds it
- [ ] Unit tests for selection (profile always included, threshold respected)
