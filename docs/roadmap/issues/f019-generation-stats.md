# f019 Show generation stats in chat (time-to-first-token, tokens/sec, backend)
Labels: enhancement, good first issue
**Milestone:** M2 Core Experience · **Priority:** P2 · **Size:** S · **Area:** Chat / Engine
**Related:** #42 (bubble-only backend label)

**Title:** Optional per-message performance footer for power users

**Description:**
#42 adds the backend label to the bubble. The main chat has no performance visibility, so users can't tell whether NPU/GPU (#40, #41) is working or compare models.

**Proposed behavior:**
- Settings → Chat → "Show generation details" (default off)
- Under each assistant message: `1.2s to first token · 18.4 tok/s · GPU · Gemma 4 E2B`
- Token count from the engine where available; fall back to a chars/4 estimate labelled "≈"

**Acceptance Criteria:**
- [ ] Stats stored with the message (nullable columns + migration) or shown only for the current session (decide in PR)
- [ ] Backend label comes from the same `LlmRepository.activeBackendLabel()` introduced for #42
- [ ] Hidden by default; no layout change when off
