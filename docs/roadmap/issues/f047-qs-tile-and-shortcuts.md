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
- [x] Tile state shows whether the bubble is active
- [x] Shortcuts work from a cold start
- [x] Dynamic shortcuts updated when chats are deleted (no dead shortcuts)

**Status:** 🟡 Done, device check pending (2026-10-04). Quick Settings tile 'Ask Orbit' (OrbitTileService): tap triggers the bubble when it's on, otherwise opens voice mode in a new chat; active state and subtitle show whether the bubble is on; long-press opens Settings (QS_TILE_PREFERENCES). Static shortcuts: New chat, Voice, Temporary chat, Reminders. Dynamic shortcuts for the 2 most recent chats, refreshed whenever the chat list changes; pinned shortcuts to deleted chats are disabled. MainActivity turns these intents into a LaunchRequest handled after onboarding. Device check pending: cold-start shortcuts and the tile.
