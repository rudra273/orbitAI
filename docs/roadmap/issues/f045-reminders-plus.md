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
- [x] `LocalReminderParserTest` extended with every new phrase pattern
- [x] Repeat rules stored in a forward-compatible format (RRULE-like string) with a migration
- [x] Rescheduling after reboot (`ReminderRestoreReceiver`) handles every new repeat type
- [x] DST and time-zone change tests

**Status:** 🟡 Done, device check pending (2026-10-04). RepeatRule stores repeats as RRULE-style strings (FREQ=HOURLY/DAILY/WEEKLY[;INTERVAL][;BYDAY], FREQ=MONTHLY;BYMONTHDAY); DB v19 (MIGRATION_18_19) converts DAILY/WEEKLY. The parser understands every weekday/weekend, every mon and wed, the 1st of every month / every 20th, every N hours/days, morning/tonight. Restore after reboot goes through the same RepeatRule.next for every type. Snooze: 10 min / 1 hour on the notification (Android shows 3 actions, plus Done) and 10 min / 1 hour / tomorrow morning in the list. 'Remind me about this' on user messages (long-press) and replies (alarm icon). Reminders list grouped Today / Upcoming / Repeating / Done. Tests cover new phrases, DST and a time-zone change (a reminder keeps the same moment after the zone changes).
