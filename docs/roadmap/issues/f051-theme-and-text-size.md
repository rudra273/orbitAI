# f051 Theme options: follow system, dynamic colour, pure black, chat text size
Labels: enhancement, good first issue
**Milestone:** M2 Core Experience · **Priority:** P2 · **Size:** M · **Area:** UI

**Title:** Respect the system theme and let users tune readability

**Description:**
`ThemeSettingsStore.isDarkTheme` is a boolean that defaults to `true`. There's no "follow system" option, no Material You dynamic colour, no OLED black, and chat text size can only change through the system font scale.

**Proposed behavior:**
- Theme: **System** (new default for fresh installs) / Light / Dark / Pure black
- Dynamic colour toggle (Android 12+), keeping the Orbit brand palette as default
- Chat text size slider (90–130%) applied to message bubbles and the code font
- Message density: comfortable / compact

**Acceptance Criteria:**
- [ ] Existing users keep their current explicit choice (migrate `is_dark_theme` → explicit Light/Dark)
- [ ] Bubble overlay theme follows the same setting (`BubbleThemeConfig`)
- [ ] All screens checked for contrast in all four themes (f052)
