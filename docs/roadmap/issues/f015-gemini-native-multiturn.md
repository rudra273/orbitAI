# f015 Send Gemini real multi-turn content and system instructions (not Gemma-formatted text)
Labels: bug, enhancement
**Milestone:** M1 Foundation · **Priority:** P0 · **Size:** M · **Area:** Engine / Cloud

**Title:** Build Gemini requests from structured history instead of a Gemma prompt string

**Description:**
`geminiChatModelOrNull()` creates the Gemini `LlmModel` without a `promptStyle`, so it defaults to `PromptStyle.GEMMA`. `ChatViewModel` then builds a Gemma-formatted string (`<start_of_turn>user ... <end_of_turn>`). `GeminiApiEngine` sends that whole string as **one user `Part`**. Gemini therefore receives the literal special tokens and a fake transcript instead of real turns. The mode system prompt and safety policy arrive as user text rather than `systemInstruction`, and image tokens and history can't be weighted correctly.

**Proposed behavior:**
- Add `PromptStyle.NATIVE` (or a `structured` flag) for providers that accept message lists
- Extend `InferenceInput` with an optional structured form: `systemInstruction`, `turns: List<Turn(role, text, images)>`, `context` (RAG + memories)
- `GeminiApiEngine` maps it to `GenerateContentConfig.systemInstruction` + a `List<Content>` with `user`/`model` roles
- Local engines keep using `ModelPromptBuilder`

**Acceptance Criteria:**
- [x] Gemini requests contain no `<start_of_turn>`/`<end_of_turn>` text (unit test on the request builder)
- [x] Mode system prompt and `AiSafetyPolicy` are sent as `systemInstruction`
- [x] Images attach to the correct user turn
- [x] The cloud-consent dialog still appears before any Gemini call
- [x] Context budgeting (f011) also applies to Gemini

**Status:** ✅ Done (2026-10-03). `InferenceInput` gains an optional `structured: StructuredPrompt` (system instruction + user/model turns), filled for every chat and tool-draft request. `GeminiApiEngine` sends it as `systemInstruction` + real `Content` turns via the pure `buildGeminiContents` (images on the last user turn, empty turns dropped, same-role turns merged, history starts with a user turn). Local engines are unchanged. 5 tests in `GeminiContentsTest`. Cloud-consent flow untouched; f011 budgeting applies to Gemini (32k). Note: no live Gemini call was made here (no API key on this machine), so do one smoke test before release.
