# f014 Stopping a reply shouldn't unload the model
Labels: bug, enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Engine
**Depends on:** f008

**Title:** Cancel generation in-engine instead of closing and reloading the model

**Description:**
`ChatViewModel.stopGeneration()` cancels the job and calls `llmRepo.close()` (twice, in `engineCloseJob`). `sendMessageForChat()` also closes the engine whenever a previous generation was still running. So **every Stop, or every message sent while one is streaming, forces a full model reload** on the next turn: several seconds for a 2–4 GB LiteRT-LM model, and longer on CPU fallback.

The close-to-cancel approach was likely added because some backends kept decoding after coroutine cancellation.

**Proposed behavior:**
- Add `cancelGeneration()` to `LlmInferenceEngine`, implemented per engine:
  - LiteRT-LM: close the in-flight `Conversation` only, keep the `Engine`
  - ONNX GenAI: stop the generator loop (already checks `emittedTokenCount`), dispose the `Generator` only
  - MediaPipe: use the session/async cancel API if available; otherwise keep today's close as the fallback for this engine only
  - Gemini: cancel the HTTP stream
- `stopGeneration()` calls `cancelGeneration()`, not `close()`

**Acceptance Criteria:**
- [ ] Stop → send again on a LiteRT-LM model starts streaming without "Loading model"
- [ ] No ghost tokens from the stopped reply appear in the new reply (the generation-token guard stays)
- [ ] No native crash after 20 rapid stop/send cycles on GPU backend
- [ ] Engine is still fully released on `onCleared()` and on memory pressure (`onTrimMemory`)
