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
- [x] No overlay permission needed (this is an activity, not the bubble)
- [x] Preferred target language set in Settings
- [x] If no local model is downloaded, a clear CTA to download one

**Status:** 🟡 Done, device check pending (2026-10-04). ProcessTextActivity is now a Compose sheet: Proofread, Rewrite (formal / friendly / shorter), Translate (Settings > Chat > Translate to), Summarize, Explain, Reply, plus Ask Orbit and Create reminder. Results stream from the shared on-device model (never Gemini); editable selections get Replace (returns EXTRA_PROCESS_TEXT), read-only ones Copy / Share. No local model: a Download a model button. No overlay permission involved. Device check pending: Replace in Gmail, WhatsApp, Chrome.
