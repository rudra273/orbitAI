# f041 New device tools: timers, alarms, SMS, navigation, open app, web search, calendar events
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** L · **Area:** Automation
**Depends on:** f040

**Title:** Expand what Orbit can do on the phone, all via standard Android intents with confirmation

**Description:**
Current device actions: email draft, WhatsApp draft, reminders, calls, memory. The most common assistant requests ("set a timer for 10 minutes", "wake me at 6", "navigate to the airport", "open Spotify", "text Rahul") aren't supported.

**Proposed tools (each an `OrbitTool`, f040):**

| Tool | Mechanism | Permission |
|---|---|---|
| Set timer | `AlarmClock.ACTION_SET_TIMER` | `com.android.alarm.permission.SET_ALARM` (normal) |
| Set alarm | `AlarmClock.ACTION_SET_ALARM` | same |
| SMS draft | `ACTION_SENDTO` `smsto:` + body (user presses Send) | none |
| Navigate / find place | `geo:` URI with query | none |
| Open app | `PackageManager` launch intent by fuzzy app-name match | `<queries>` for launcher intents |
| Web search | `Intent.ACTION_WEB_SEARCH` | none |
| Calendar event | `CalendarContract` insert intent (already used for calendar handoff) | none |
| Flashlight on/off | `CameraManager.setTorchMode` | none |

**Acceptance Criteria:**
- [x] Each tool has a deterministic command form and works via native tool calling
- [x] Each tool shows a confirmation card before launching
- [x] Parser unit tests per tool (English; Hinglish phrasing where cheap)
- [x] Settings → Tools lists each tool with an enable toggle
- [x] No tool sends a message or places a call without the user's final tap in the target app (current safety model kept)

**Status:** 🟡 Done, device check pending (2026-10-04). DeviceActionParser (English + common Hinglish, unit-tested) and native tools (OrbitChatToolSchema) for timer, alarm, SMS draft, navigate, open app (fuzzy AppMatcher over launcher apps, <queries> added), web search, calendar event and flashlight. Every action shows a confirmation dialog first; SMS only opens the SMS app with the text, so Send is always the user's tap. OrbitTool registry with an on/off switch per tool in Settings > Tools (also covers email, WhatsApp, reminders, calls, memory); disabled tools are skipped by parsers, refused as tool calls and hidden from the / menu. Each new tool has a / command. f040's full registry refactor is still open. Device check pending.
