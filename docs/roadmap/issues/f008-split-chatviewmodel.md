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
- [ ] `ChatViewModel.kt` ≤ 350 lines and only wires controllers to UI state
- [ ] Each controller has JVM unit tests for its state transitions, using fakes for repositories
- [ ] `memory extraction` moves out of the ViewModel (see f037)
- [ ] No behavioural change: send, stop, call, reminder, WhatsApp, email and notification-reply flows all work as before
