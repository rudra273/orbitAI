# f050 Move UI strings to resources and ship the first translations
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** L · **Area:** UI / i18n

**Status:** 🟡 First pass done, device check pending (2026-10-04). Owner chose Hindi. About 310 UI texts moved to string resources with a Hindi translation (`values-hi`), a per-app language picker (Settings > App language, via `locales_config.xml`), and lint already flags a missing translation. Still English: texts built from variables (counts, names, status lines), messages in the floating bubble, notifications, and the model prompts. The Hindi was written by me and needs a native reader to review it. Not done: pseudo-locale and RTL layout checks, CI check against new hardcoded strings, plurals.

**Title:** Externalize every user-facing string and add the first non-English locales

**Description:**
`res/values/strings.xml` contains only `app_name`. There are 100+ hardcoded `Text("…")` calls in Compose and **zero** `stringResource(...)` calls, plus hardcoded strings in ViewModels (`loadError = "…"`), notifications, the bubble overlay and `ProcessTextActivity`. The app can't be translated, and that blocks large markets where on-device AI is especially valuable (offline, data-cost sensitive).

**Proposed behavior:**
- Move all user-facing strings to `strings.xml` (ViewModels emit string resource IDs or a `UiText` wrapper, not raw strings)
- Plurals via `pluralStringResource`
- First locales chosen by the owner (suggestion: Hindi + one more based on Play install data)
- Per-app language picker (Android 13+ `LocaleManager` / `locales_config.xml`)
- Prompts: tell the model to reply in the user's chosen language (setting), separate from UI language

**Acceptance Criteria:**
- [ ] Lint `HardcodedText` / a custom check prevents new hardcoded strings (enforced in CI, f004)
- [ ] Pseudo-locale (`en-XA`) build checked for truncation and layout breakage
- [ ] RTL pseudo-locale (`ar-XB`) checked (manifest already has `supportsRtl`)
- [ ] Owner confirms target locales in this issue
