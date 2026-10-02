# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Rules (from `.github/instructions/orbitaiprompt.instructions.md`)

- **Do exactly what is asked.** No unrequested features, refactors, comments, or abstractions. Keep it simple; use idiomatic Android/Kotlin.
- **Stop and ask when blocked** rather than guessing: dependency version conflicts (report the exact error, don't self-resolve), missing URLs/endpoints (never fabricate), and critical architectural/design decisions.
- **No workarounds.** Never use `--force`, `--no-verify`, suppressions, or hacks to bypass a failing step. Surface the exact error.
- **Don't loop.** If a task isn't progressing after one or two attempts, stop and report what's failing.

## Build, Run, Test

OrbitAI is a single-module Android app (`:app`). Dependencies are managed via a version catalog (`gradle/libs.versions.toml`); some are declared directly in `app/build.gradle.kts`. `minSdk 35`, `targetSdk 36`, JVM 11.

```sh
# Install & launch on a connected device/emulator
./gradlew installDebug && adb shell monkey -p com.example.orbitai -c android.intent.category.LAUNCHER 1

# Logs
adb logcat | grep orbitai   # or filter by tag, e.g. ChatViewModel

# Unit tests (JUnit, JVM-local — the parser/prompt/router suites live here)
./gradlew test
./gradlew testDebugUnitTest --tests "com.example.orbitai.tools.router.ToolRouterTest"

# Instrumented tests (require a device/emulator)
./gradlew connectedAndroidTest

# Release builds
./gradlew assembleRelease   # unsigned APK
./gradlew bundleRelease     # signed .aab (requires keystore.properties — see README)
```

Release signing reads `keystore.properties` at the repo root (gitignored). Without it, release builds are unsigned. Version name/code are derived from `ORBIT_RELEASE_VERSION`/`ORBIT_RELEASE_CODE` env vars or `orbitReleaseVersion`/`orbitReleaseCode` gradle properties (see `parseReleaseVersion*` in `app/build.gradle.kts`). CI (`.github/workflows/build-release.yml`) builds unsigned on PRs to `main` and signed releases on `v*` tags.

## Architecture

On-device AI chat assistant. All inference runs locally except the optional Gemini cloud provider. UI is Jetpack Compose; persistence is Room; there is no DI framework — dependencies are instantiated directly (e.g. `ChatViewModel` news up all its repositories/stores).

### Inference engine abstraction (`core/engine`)

The central abstraction. `LlmRepository` is the single entry point for loading a model and streaming inference, used by **both** the main app and the floating bubble. It caches one active engine and reloads only when the model id or `InferenceSettings` change.

- `LlmInferenceEngineFactory.create()` picks an engine by `ModelProvider` + `ModelFormat`:
  - `GEMINI` → `GeminiApiEngine` (cloud, needs API key)
  - `TASK` → `MediaPipeTaskEngine`
  - `LITERTLM` → `LiteRtLmEngine`
  - `ONNX_GENAI` → `OnnxGenAiEngine`
- `LlmInferenceEngine` is the base interface: `generateResponseStream(InferenceInput, maxDecodedTokens): Flow<String>`.
- `LlmConversationEngine` / `ConversationSession` add multi-turn + tool-calling, **currently only implemented by `LiteRtLmEngine`**. `LlmRepository.conversationEngine()` returns null for other formats — check `supportsConversations()` before using it.
- Local model files live under `getExternalFilesDir(null)/models/<fileName>` (sideloaded via ADB or downloaded in-app). `resolveModelPath` enforces this and gives actionable "delete and redownload" errors.

The model catalog is a hardcoded list in `core/model/Models.kt` (`AVAILABLE_MODELS`). `availableChatModels()` (`core/model/ModelAvailability.kt`) returns Gemini (if `TokenStore.hasGeminiConfig()`) plus locally-downloaded models. Adding a model = adding a `LlmModel` entry with the right `format`/`promptStyle`/`supportsVision`.

### Prompt building (`core/prompt`)

`ModelPromptBuilder` formats prompts per `PromptStyle` (GEMMA/PHI/LLAMA3/QWEN) — each has different turn/special tokens. `buildChatPrompt` assembles history + RAG context + memories + system prompt; `wrapInstructionPrompt` wraps a single instruction (used by automation drafts). When adding a model with a new prompt family, extend `PromptStyle` and both functions.

### Chat orchestration (`viewmodel/ChatViewModel.kt`)

The largest orchestration surface — read this to understand the end-to-end message flow. `sendMessage()`:
1. Cancels any in-flight generation (uses a monotonic `activeGenerationToken` to ignore stale streams; also closes the engine so the backend tears down cleanly).
2. Routes the input through `AutomationRouter` → either a tool request or normal chat.
3. Resolves the model (chat's model → last-selected → first available), validates vision support for image input.
4. Persists the user message, optionally auto-extracts memory facts (regex patterns in `extractMemoryFacts`).
5. Builds the prompt: for normal chat, pulls RAG chunks from active spaces + memories + the active mode's system prompt; for tool requests, uses the corresponding draft prompt builder.
6. Streams the response into a placeholder assistant message, then (for tool requests) parses the model output and executes the automation.

Active **mode** (`activeModeId`) selects the system prompt and per-mode `InferenceSettings`; active **spaces** (`activeSpaceIds`) scope RAG retrieval.

### Automation / tool-calling (`feature/automation`)

Command-driven, not free-form tool calling. Flow: `AutomationRouter.route()` → `AutomationCommandParser.parse()` matches regex patterns (`/mail`, `/whatsapp`, `/remind`, and natural-language equivalents) to an `AutomationRequest`. If matched, the chat is "tool-only": the LLM generates a structured draft using a draft prompt builder, then a draft parser (`EmailDraftParser`, `WhatsAppDraftParser`, `ReminderDraftParser`) extracts fields from the model output, and `AutomationExecutor` fires an Android Intent (or `ReminderScheduler` schedules an alarm). Some paths need runtime permissions (contacts for WhatsApp-by-name, notifications for reminders) — the ViewModel emits `ChatUiEvent` and stashes a `Pending*Execution` to resume after the grant.

The unit tests under `app/src/test/.../tools/` cover this subsystem (router, draft parsers, draft prompt builders) — keep them green when touching parsing/prompt logic.

### Floating bubble (`feature/bubble/OrbitBubbleService.kt`)

A `SYSTEM_ALERT_WINDOW` overlay service (the largest single file, ~1800 lines) providing an always-available assistant outside the app. Started/stopped from `MainActivity` based on overlay permission and app-foreground state. Uses its own `LlmRepository` instance and a separate tool schema (`OrbitBubbleToolSchema`). Screen-context features go through `MediaProjectionPermissionActivity`; text selection through `ProcessTextActivity`.

### Data layer (`core/database`)

Single Room DB `orbitai.db` (`AppDatabase`, currently **version 11**) with entities split across files: chats/messages (`Entities.kt`), RAG docs/chunks with embeddings (`RagEntities.kt`), memories (`MemoryEntities.kt`), spaces (`SpaceEntities.kt`), modes (`ModeEntities.kt`). **All schema changes require a hand-written `Migration` appended to the `addMigrations(...)` chain** (`exportSchema = false`, no auto-migrations). Default modes (Orbit/Concise/Step-by-step) are seeded via migration SQL.

### RAG & Spaces (`feature/spaces`)

"Spaces" are user knowledge bases. Documents (PDF via PDFBox, text) are chunked, embedded with the on-device `EmbeddingModel` (Universal Sentence Encoder `.tflite`), and stored as BLOBs in `rag_chunks`. `SpaceRepository.searchChunksInSpaces()` does cosine-similarity retrieval scoped to the active space ids, feeding the top-K chunks into the chat prompt.

### Settings stores (`core/common`)

Lightweight `SharedPreferences` wrappers, one concern each: `TokenStore` (HF token, Gemini API key/model, last-selected model), `ModeInferenceSettingsStore` (per-mode `InferenceSettings`), plus theme/onboarding/bubble/automation stores. No shared prefs schema abstraction — each store owns its keys.

## Conventions

- Package layout is `core/*` (engine, model, prompt, database, common) for infrastructure and `feature/*` for user-facing features; UI is `ui/screens`, `ui/navigation`, `ui/theme`; ViewModels in `viewmodel/`.
- Long-running work runs on `Dispatchers.IO` inside `viewModelScope`; generation cancellation uses the token pattern, not just `Job.cancel()`.
- The `applicationId`/`namespace` is `com.example.orbitai` (kept as-is despite the `example` prefix — don't rename without cause).
