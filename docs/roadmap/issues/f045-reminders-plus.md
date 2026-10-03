# f045 Reminders+: monthly and weekday repeats, custom snooze, reminder from any message
Labels: enhancement, good first issue
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M · **Area:** Reminders

**Title:** Close the gaps in Orbit's own reminder manager

**Description:**
Reminders support `NONE`/daily/weekly repeat and a fixed 10-minute snooze (`ReminderScheduler.snooze`). Common phrases like "every weekday at 9", "on the 1st of every month" or "every 2 hours" fail or get misparsed by `LocalReminderParser`.

**Proposed behavior:**
- Repeat options: weekdays, specific days of week, monthly (by date), every N hours/days
- Snooze choices on the notification: 10 min / 1 hour / tomorrow morning
- Long-press any chat message → "Remind me about this"
- Reminder list grouped: Today / Upcoming / Repeating / Done

**Acceptance Criteria:**
- [ ] `LocalReminderParserTest` extended with every new phrase pattern
- [ ] Repeat rules stored in a forward-compatible format (RRULE-like string) with a migration
- [ ] Rescheduling after reboot (`ReminderRestoreReceiver`) handles every new repeat type
- [ ] DST and time-zone change tests
