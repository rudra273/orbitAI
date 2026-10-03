# f011 Fit prompts to the model's context window
Labels: bug, enhancement
**Milestone:** M1 Foundation · **Priority:** P0 · **Size:** M · **Area:** Engine / Prompt

**Title:** Budget history, RAG chunks and memories against a per-model context limit

**Description:**
`ChatViewModel.sendMessageForChat()` passes the **entire** chat history (`chatRepo.getChat(chatId)?.messages`) to `ModelPromptBuilder.buildChatPrompt()`, plus up to 5 RAG chunks of ~500 words each, **all** memories and the mode system prompt. Nothing is ever trimmed. On small models (Gemma 3 1B, Gemma 3 270M) a long chat or one Space query overflows the context. Depending on the engine, users see a load/generation error, silent truncation of the *newest* turn, or incoherent answers.

(The automation draft builders already cap history with `messages.takeLast(10)`; normal chat does not.)

**Proposed behavior:**
- Add `contextTokens: Int` to `LlmModel` (and to the Gemini synthetic model) in `core/model/Models.kt`
- Add a `PromptBudgeter` in `core/prompt/` that estimates tokens (chars/4 heuristic, or the engine tokenizer when available, e.g. ONNX `Tokenizer.encode`)
- Fill in priority order: system prompt → newest user turn → RAG context → relevant memories → as many recent turns as fit, newest first
- Reserve `maxDecodedTokens` for the response
- When turns are dropped, show a subtle "Earlier messages aren't in context" marker in the chat

**Acceptance Criteria:**
- [x] Every catalog model has a `contextTokens` value
- [x] Prompt (estimated) + `maxDecodedTokens` ≤ `contextTokens` for all prompt styles (unit tests in `app/src/test/.../core/prompt/`)
- [x] The newest user message is never dropped; an oversized single message is truncated with a visible notice
- [ ] Manual test: a 100-message chat on Gemma 3 1B still answers the latest question coherently

**Status:** 🟡 Done, pending device check (2026-10-03). New `core/prompt/PromptBudget` trims history (newest first), RAG chunks and memories to `LlmModel.contextTokens` (conservative values per model; Gemma 2 = 1280 from its `ekv1280` file; Gemini 32k); the output reserve is capped at half the context. Wired into normal chat in `ChatViewModel`. The 'earlier messages aren't in context' marker is a one-time info banner per chat, and a truncated long message shows its own banner. 6 unit tests in `PromptBudgetTest`, including every catalog model × every prompt style. ⚠️ Manual 100-message test on Gemma 3 1B still needs a device.
