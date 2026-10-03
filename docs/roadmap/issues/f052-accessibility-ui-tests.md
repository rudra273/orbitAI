# f052 Accessibility pass and Compose UI tests for core flows
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P1 · **Size:** M · **Area:** UI / Quality

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
