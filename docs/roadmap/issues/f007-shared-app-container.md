# f007 Share one `LlmRepository` and repository set across the app and the bubble
Labels: enhancement, bug
**Milestone:** M1 Foundation · **Priority:** P1 · **Size:** L · **Area:** Architecture
**Depends on:** #39

**Title:** Introduce an application-level container so a model is loaded at most once per process

**Description:**
Dependencies are created ad hoc. `ChatViewModel` creates its own `LlmRepository`, `SpaceRepository`, `ChatRepository` and `ModelDownloader`. `SpacesViewModel` creates another `SpaceRepository`. `OrbitBubbleService` lazily creates a separate `bubbleLlmRepo` and closes it only in `onDestroy()`.

**Confirmed problem:** when the app comes to the foreground, the bubble is removed but the service, and the model it loaded, stays alive. The chat screen then loads its own copy. With a 2–4 GB model, two engines in RAM is a likely low-memory kill on 8 GB phones. Each `SpaceRepository`/`ChatRepository` also starts its own `CoroutineScope` and eager `stateIn` collectors.

**Decision needed:** a manual `AppContainer` held by an `Application` subclass (no new dependency, matching today's "no DI framework" convention), or Hilt. Recommendation: manual container.

**Acceptance Criteria:**
- [x] `OrbitApplication` holds a single `LlmRepository`, `ChatRepository`, `SpaceRepository`, `MemoryRepository`, `ModeRepository`, `ModelDownloader`, `ReminderScheduler`
- [x] ViewModels and `OrbitBubbleService` get instances from the container instead of constructing them
- [x] `LlmRepository` access is serialized (a `Mutex`) so app and bubble can't load or close concurrently
- [ ] Only one model is resident in memory at a time (verify with `adb shell dumpsys meminfo com.example.orbitai`)
- [x] CLAUDE.md "no DI framework" section updated to describe the container

**Status:** 🟡 Done, pending device check (2026-10-04). Manual container chosen (no new dependency): `OrbitApplication` + `core/AppContainer` hold single `LlmRepository`, `ChatRepository`, `SpaceRepository`, `ModeRepository`, `MemoryRepository`, `ModelDownloader`, `ReminderScheduler`. ViewModels, the bubble, `ModelDownloadWorker` and reminder receivers use `context.appContainer`. `LlmRepository` load/close are `@Synchronized`; inference holds a lease (`beginUse`/`endUse`) and `releaseIfIdle()` is used on ViewModel clear, bubble destroy and `onTrimMemory(BACKGROUND)`. Logs from 2026-10-03 had shown the app loading a second Gemma 4 copy on GPU while the bubble's was resident; that path is now impossible. CLAUDE.md updated. ⚠️ Confirm on device with `adb shell dumpsys meminfo com.example.orbitai` after using chat, then the bubble.
