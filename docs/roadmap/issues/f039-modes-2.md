# f039 Modes 2.0: per-mode model and Spaces, templates gallery, import/export
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M · **Area:** Modes

**Title:** Make Modes full presets, not just system prompts

**Description:**
A Mode today is `name + systemPrompt + isActive`, with per-mode `InferenceSettings` in `ModeInferenceSettingsStore`. Users who build a "Lawyer" or "Code reviewer" mode still have to pick the right model and Spaces by hand every time, and can't share a mode they've tuned.

**Proposed behavior:**
- Optional per-mode **default model** and **default Spaces** (applied when the mode is selected; overridable per chat)
- Icon + accent colour per mode, shown on the mode chip
- **Templates gallery:** Translator, Code helper, Tutor (explain step by step), Writing coach, Summarizer, Interview practice, Fitness coach. One tap to add, then editable
- **Import/Export** a mode as a small JSON file (share sheet)
- Mode picker in the bubble uses the same data (existing `BUBBLE_MODE_ID` handling kept)

**Acceptance Criteria:**
- [x] Migration adds columns to `modes`; migration test (f005)
- [x] Imported JSON validated (size limit, required fields); bad files show a clear error
- [x] Selecting a mode with a default model that isn't downloaded falls back gracefully with a message

**Status:** 🟡 Done, device check pending (2026-10-04). DB v20 (MIGRATION_19_20): modes.defaultModelId / defaultSpaceIds / accentColor (MigrationTest extended). Mode editor: 'When this mode is selected' sets a model (or keep the chat's), Spaces to turn on, and a chip colour; selecting the mode in chat applies them, and a model that isn't downloaded keeps the current one with a message. Templates gallery (Translator, Code helper, Tutor, Writing coach, Summarizer, Interview practice, Fitness coach). Share this mode exports a .orbitmode.json file; Import validates format, size (32 KB) and required fields with clear errors and clamps values (ModeJsonTest). Icons use the existing leading emoji in the name. The bubble keeps its own mode handling.
