# f021 Import a custom local model file
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M · **Area:** Models

**Status:** 🟡 Done, device check pending (2026-10-04). Registry, importer (storage check, copy, test load, cleanup) and the Settings > Models "Import model file" screen (name, prompt style, context size, vision, progress, error, delete, Custom tag).

**Title:** Let power users add their own `.litertlm` / `.task` model from device storage

**Description:**
The only ways to add a model are the hardcoded catalog or `adb push` into `models/` with an exact filename. Power users who convert or fine-tune their own LiteRT-LM models have no in-app path.

**Proposed behavior:**
- Model screen → "Import model file" (SAF `OpenDocument`, filter `.litertlm`, `.task`)
- Copy into `getExternalFilesDir(null)/models/custom/` with a progress indicator
- User sets display name, prompt style (Gemma / Phi / Llama 3 / Qwen), vision on/off, context size
- Custom models persist in a small Room table or JSON file and appear in the model picker with a "Custom" tag
- Try loading once after import; show the native error if it fails

**Acceptance Criteria:**
- [ ] Imported model survives app restart and can be deleted
- [ ] Invalid file → clear error, no crash, file removed
- [ ] Custom models work in chat and in the bubble
- [ ] Storage check before copy (shared with f017)
