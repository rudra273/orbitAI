# f008 Split `ChatViewModel` (1,034 lines) into focused controllers
Labels: enhancement
**Milestone:** M1 Foundation · **Priority:** P1 · **Size:** L · **Area:** Architecture
**Depends on:** f007

**Title:** Decompose `ChatViewModel` the same way #39 decomposes the bubble service

**Description:**
`ChatViewModel` holds five unrelated flows: chat generation, the voice/typed call conversation, reminder review and scheduling, WhatsApp/email automation execution, and notification-reply drafting. Each has its own pending state (`pendingWhatsAppExecution`, `pendingReminderExecution`, `pendingCallLookup`, `pendingCall`, `notificationGenerationToken`). Almost every 2.0 chat feature (f011, f014, f016, f022, f027, f040) touches this file. It needs splitting before that work starts.

**Proposed split (all in `viewmodel/` or `feature/*`):**

| New class | Moves from `ChatViewModel` |
|---|---|
| `ChatGenerationController` | `sendMessageForChat`, prompt building, streaming, generation-token logic, `stopGeneration` |
| `CallFlowController` | `resolveCall`, `handleVoiceCall`, `confirmCall`, call review state |
| `ReminderFlowController` | reminder review, `confirmReminder`, snooze/complete/cancel |
| `AutomationFlowController` | post-generation draft parsing and `AutomationExecutor` handoff, permission resumes |
| `NotificationReplyViewModel` | `draftNotificationReply` and its state (used only by `NotificationRepliesScreen`) |

**Acceptance Criteria:**
- [x] `ChatViewModel.kt` ≤ 350 lines and only wires controllers to UI state
- [ ] Each controller has JVM unit tests for its state transitions, using fakes for repositories
- [x] `memory extraction` moves out of the ViewModel (see f037)
- [ ] No behavioural change: send, stop, call, reminder, WhatsApp, email and notification-reply flows all work as before

**Status:** 🟡 Done, pending device check (2026-10-04). `ChatViewModel` is now 132 lines and only wires controllers to the UI (public API unchanged, so no screen edits). New in `viewmodel/`: `ChatGenerationController` (send, prompt, stream, stop, watchdog, model/mode/space selection, cloud consent, `launchExclusive` generation slot), `CallFlowController`, `ReminderFlowController`, `AutomationFlowController` (typed device commands + post-generation draft execution + contacts resume), `NotificationReplyController`, `ChatControllerContext` (shared UI state/events/scope) and `ChatUiModels`. Memory regex moved to `feature/memory/MemoryFactExtractor` with 4 tests that pin current behaviour (incl. the false positives f037 removes). Deviation: notification replies are a controller, not a separate ViewModel, because they must stop chat generation before using the shared model. Code moved verbatim. Build, 79 unit tests, 11 device tests and lint pass; app launches without crashes. Open: controller unit tests need fake repositories (repositories are concrete Room-backed classes); best done with the fake engine in f052. ⚠️ Manually run send/stop/call/reminder/WhatsApp/email/notification-reply once on device.
