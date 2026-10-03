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
- [ ] The command list is generated from one registry shared with the parsers (no duplicated strings), ready for f040
- [ ] Saved prompts persisted (Room table + migration, or a JSON store)
- [ ] Keyboard and TalkBack navigable popup
- [ ] Disabled tools (Settings → Tools) don't appear
