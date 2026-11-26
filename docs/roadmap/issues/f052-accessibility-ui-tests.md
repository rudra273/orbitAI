# f052 Accessibility pass and Compose UI tests for core flows
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P1 · **Size:** M · **Area:** UI / Quality

**Status:** 🟡 Partly done, device check pending (2026-10-04). Done: every switch row now reads and toggles as one control with a Switch role (Tools, Memory, Bubble, Chat, Appearance, Backup, Digest, Message replies, custom model); show/hide token button has a label; back rows are at least 48 dp tall; a finished reply is announced once politely ("Reply ready") instead of on every word; a Compose UI test for the What's new dialog (run with installDebugAndroidTest). The icons left without a description sit beside their own text and are decorative. Not done: Accessibility Scanner pass and screenshots, 200% font-scale check, contrast audit, bubble overlay accessibility actions, a fake engine, and UI tests for send/stop/mode/Space flows.

**Title:** Make OrbitAI fully usable with TalkBack and protect key flows with UI tests

**Description:**
For a voice-capable assistant, accessibility is core. There are no Compose UI tests, and icon-only buttons (mic, send, stop, attach, mode/space chips, bubble radial menu) need checking for labels, focus order and 48 dp touch targets.

**Scope:**
- Content descriptions for every icon button; `semantics { role/stateDescription }` for toggles and chips
- Streaming replies announced politely (live region on completion, not on every token)
- Touch targets ≥ 48 dp; contrast ≥ 4.5:1 in all themes (f051)
- Bubble overlay views: `contentDescription` + accessibility actions
- Font scale 200%: no clipped text in chat, settings, model cards

**UI tests (`androidTest`, `createAndroidComposeRule`):** send a message with a fake engine; stop generation; switch mode; toggle a Space; reminder review dialog confirm; download card states.

**Acceptance Criteria:**
- [ ] Accessibility Scanner shows no errors on the main screens (screenshots in PR)
- [ ] A fake `LlmInferenceEngine` exists for tests (also used by f008 unit tests)
- [ ] UI tests run in CI on an emulator job (nightly is acceptable if too slow per PR)
