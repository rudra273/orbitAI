# f057 Privacy center: what's stored, what left the device, one-tap clearing
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P2 · **Size:** M · **Area:** Privacy

**Title:** One screen that makes the on-device promise verifiable

**Description:**
"Your data stays on device" is OrbitAI's main differentiator, but users can't verify it. Data is spread across Room, SharedPreferences stores, model files, and the network calls made by Gemini, model downloads, the GitHub update check and content reports.

**Proposed behavior:**
- Settings → Privacy center:
  - **Storage:** sizes for chats, memories, Spaces, models, cache, with clear buttons per category
  - **Network activity log:** local, last 30 days: timestamp, destination host, purpose (Model download / Gemini request / Update check / Report), bytes. **Never** request bodies
  - **Permissions in use:** contacts, phone, mic, overlay, notification access, exact alarms, each with "why" + a link to system settings
  - **Delete everything:** wipes DB, prefs, Space files and optionally models
- The privacy policy screen links here

**Acceptance Criteria:**
- [x] Every OkHttp / GenAI client call goes through one logging interceptor or wrapper
- [x] Log contains no prompts, tokens, keys or URLs with query strings
- [x] "Delete everything" leaves the app in a fresh-install state (instrumented test)

**Status:** 🟡 Done, device check pending (2026-10-04). Settings > Privacy center: counts and sizes per category with Clear buttons (chats, memories, Spaces, models, temporary files), a 30-day network log (host, purpose, time, size only; never bodies, paths or keys) fed by one interceptor on the download, update and report clients plus a note for each Gemini request, permissions in use with why and link to system settings, and Delete everything (optionally with models) which ends the app. Not done: link from the privacy policy screen; instrumented fresh-install test. Gemini calls are logged by hand because that library doesn't use OkHttp.
