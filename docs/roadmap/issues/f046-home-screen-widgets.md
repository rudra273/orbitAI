# f046 Home-screen widgets: Ask Orbit, voice, upcoming reminders
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** L · **Area:** System integration

**Title:** Glance widgets for one-tap access to Orbit from the home screen

**Description:**
There are no widgets. For an assistant, getting in fast is the main way to make it a habit. The bubble needs overlay permission, which many users decline.

**Proposed widgets (Jetpack Glance):**
1. **Ask Orbit (4×1):** search-bar style; tap → new chat with keyboard open; mic icon → new chat in voice mode (f024); camera icon → new chat with camera (f029)
2. **Reminders (2×2 / 4×2):** next 3 Orbit reminders with a Done action
3. **Quick prompts (2×2):** 4 user-chosen saved prompts (f028)

**Acceptance Criteria:**
- [ ] Widgets follow the app theme and Material You colours on Android 12+
- [ ] Reminder widget refreshes on create/complete/snooze and at midnight
- [ ] Widget actions deep-link through `MainActivity` without duplicating chats (reuse `findReusableEmptyChatId`)
- [ ] Widget preview images/layouts in the picker
