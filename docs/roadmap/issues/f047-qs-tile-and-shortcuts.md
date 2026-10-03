# f047 Quick Settings tile and launcher shortcuts
Labels: enhancement, good first issue
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** S · **Area:** System integration

**Title:** Reach Orbit from the notification shade and from a long-press on the app icon

**Description:**
Besides the launcher icon and the optional bubble, there's no quick entry point.

**Proposed behavior:**
- **Quick Settings tile** (`TileService`): tap → start voice ask (bubble if enabled, else open voice mode); long-press → Orbit settings
- **Static launcher shortcuts** (`res/xml/shortcuts.xml`): New chat · Voice · New temporary chat (f027) · Reminders
- **Dynamic shortcuts:** the 2 most recent chats (`ShortcutManagerCompat`)

**Acceptance Criteria:**
- [ ] Tile state shows whether the bubble is active
- [ ] Shortcuts work from a cold start
- [ ] Dynamic shortcuts updated when chats are deleted (no dead shortcuts)
