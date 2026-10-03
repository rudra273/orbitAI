# f043 Calendar awareness: "What's on my day?"
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M · **Area:** Automation / Context
**Depends on:** f040

**Title:** Opt-in read access to calendar events so Orbit can answer schedule questions locally

**Description:**
Orbit can create calendar events (intent handoff) and reminders, but can't answer "Am I free at 4?", "What's my day like tomorrow?" or "Plan my afternoon around my meetings".

**Proposed behavior:**
- Opt-in in Settings → Tools → Calendar, with a disclosure, then the `READ_CALENDAR` runtime permission
- A `read_calendar(range)` tool returning event titles and times for the requested window (only the window, never the whole calendar)
- Combined with Orbit reminders for a merged agenda answer
- Events are injected only for the turn that asked, never stored in memory or chat context beyond the answer

**Acceptance Criteria:**
- [ ] Without permission the tool is not offered to the model
- [ ] Event data never leaves the device unless the user is on Gemini and accepted the cloud consent for that message (consent text updated to mention calendar data)
- [ ] Unit tests for date-range parsing ("tomorrow", "this week", "next Monday")
