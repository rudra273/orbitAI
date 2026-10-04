# f020 Refresh the model catalog for 2.0 (Qwen and newer small models)
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M · **Area:** Models

**Status:** 🟡 Partly done (2026-10-04). Added Qwen 3.5 0.8B (text) and 0.8B Vision, pinned to a verified revision with sizes and SHA-256 recorded in docs/model-catalog-audit.md, plus a Qwen prompt-format test. Per the owner, no other models for now. Still to check on a device: load, chat and vision.

**Title:** Add newer small instruct models, including a first `PromptStyle.QWEN` model

**Description:**
`PromptStyle.QWEN` and its prompt builder exist, but no catalog model uses them. The catalog also mixes legacy `.task` duplicates (Gemma 3 1B is listed twice) and has confusing IDs (`gemma3-4b` is actually Gemma 3n E4B).

Per project rules, **download URLs must not be guessed**: the owner provides each verified Hugging Face repo + pinned revision, and `docs/model-catalog-audit.md` is updated the same way it was on 2026-10-02.

Candidates to evaluate (only where a LiteRT-LM or ONNX GenAI mobile build exists): small Qwen instruct models, newer Gemma variants, and a small code- or reasoning-tuned model.

**Acceptance Criteria:**
- [ ] Owner-supplied repos and revisions recorded in `docs/model-catalog-audit.md` with sizes
- [ ] At least one `PromptStyle.QWEN` model added, with a prompt-format unit test
- [ ] Each new model manually tested for load + chat (+ vision if flagged)
- [ ] Display names clarified (no `gemma3-4b` ID showing "Gemma 3n E4B"); existing IDs and file names kept so installed models still work
- [ ] Decision recorded on hiding the legacy `.task` duplicates from new installs
