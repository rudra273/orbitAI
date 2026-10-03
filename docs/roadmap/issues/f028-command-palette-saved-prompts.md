# f028 Command palette and saved prompts ("/" menu)
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Chat / Discoverability

**Title:** Typing "/" shows every Orbit command and the user's saved prompts

**Description:**
Orbit already understands `/mail`, `/email`, `/whatsapp`, `/wa`, `/remind`, `/call`, `/remember` and natural-language forms (`AutomationCommandParser`, `DeviceCommandParser`). Users only find them by reading the README. There's also no way to save frequently used prompts ("Summarize this in 3 bullets", "Translate to Hindi", "Explain like I'm 12").

**Proposed behavior:**
- Typing `/` at the start of the input opens a filtered popup: built-in commands (icon, name, one-line description, example) + user saved prompts
- Selecting a command inserts its prefix; selecting a saved prompt inserts its text, with a `{{input}}` placeholder cursor
- Settings → Saved prompts: create, edit, reorder, delete
- The empty-chat state shows 3–4 starter suggestions based on what's set up (Spaces active, Gemini configured, tools enabled)

**Acceptance Criteria:**
- [x] The command list is generated from one registry shared with the parsers (no duplicated strings), ready for f040
- [x] Saved prompts persisted (Room table + migration, or a JSON store)
- [ ] Keyboard and TalkBack navigable popup
- [ ] Disabled tools (Settings → Tools) don't appear (no per-tool toggles exist yet; f041)

**Status:** ✅ Done (2026-10-03). New `feature/automation/OrbitCommands` registry (/mail, /whatsapp, /remind, /call, /remember with aliases, titles, examples). The slash regexes in `AutomationCommandParser` and `DeviceCommandParser` are now built from it, so names can't drift. Typing / at the start of the input (no space yet) opens a menu above the input with matching commands and saved prompts. Saved prompts (`SavedPromptStore`, JSON in SharedPreferences) are managed in Settings > Saved prompts: new, edit, move up/down, delete. Selecting a prompt inserts it with the cursor at `{{input}}` (the input field now keeps a TextFieldValue so the cursor can be placed). The empty chat shows 4 starter chips (one about documents when Spaces are active). Pure logic in `CommandPalette` with 4 tests, including one that every registry name/alias parses to the right command; all existing parser tests still pass. Note: there are no per-tool on/off switches yet (`AutomationSettingsStore.isAutomationExecutionEnabled` is unused), so 'disabled tools don't appear' has nothing to filter; it comes with f041. Not checked: keyboard/TalkBack navigation of the menu.
