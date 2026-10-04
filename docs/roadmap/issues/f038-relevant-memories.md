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
- [x] With 200 memories, prompt memory section ≤ ~150 tokens
- [x] Editing a memory re-embeds it
- [x] Unit tests for selection (profile always included, threshold respected)

**Status:** 🟡 Done, device check pending (2026-10-04). DB v16 (MIGRATION_15_16): memories.embedding/useCount/lastUsedAt, messages.usedMemories. MemorySelector sends profile facts (name, how to address, reply language) plus up to 5 relevant memories (keyword overlap or cosine >= 0.3, rank-fused) within ~150 tokens; older memories are embedded on first use. Replies show 'Memory used' (tap for the list). Settings > Memory: tap to edit (re-embeds), search, Recent / Most used, source and use count. Device check pending: instrumented MigrationTest and the UI.
