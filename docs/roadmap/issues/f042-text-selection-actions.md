# f042 Text actions in the selection menu: summarize, translate, rewrite, proofread
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** M · **Area:** System integration

**Title:** Turn `PROCESS_TEXT` into an AI writing toolkit that can replace the selected text in place

**Description:**
`ProcessTextActivity` already appears in every app's text-selection menu as "Orbit AI", but it only offers **Create reminder** / **Ask Orbit**. Android lets a `PROCESS_TEXT` activity **return replacement text** (`EXTRA_PROCESS_TEXT` in the result) when the selection is editable (`EXTRA_PROCESS_TEXT_READONLY == false`). That makes "fix my grammar in this WhatsApp reply" a two-tap flow.

**Proposed behavior:**
- Selection menu → Orbit AI → bottom sheet: **Proofread**, **Rewrite** (formal / friendly / shorter), **Translate** (to the preferred language), **Summarize**, **Explain**, **Reply to this**, **Ask Orbit**, **Create reminder**
- The result streams in the sheet using the local model (Gemini only with the existing consent dialog)
- Editable source → **Replace** button returns the text to the calling app; read-only source → **Copy** / **Share**
- Uses the shared `LlmRepository` (f007), so it doesn't double-load a model

**Acceptance Criteria:**
- [ ] Replace works in Gmail compose, WhatsApp and Chrome text fields
- [ ] No overlay permission needed (this is an activity, not the bubble)
- [ ] Preferred target language set in Settings
- [ ] If no local model is downloaded, a clear CTA to download one
