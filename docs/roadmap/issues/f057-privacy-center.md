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
- [ ] Every OkHttp / GenAI client call goes through one logging interceptor or wrapper
- [ ] Log contains no prompts, tokens, keys or URLs with query strings
- [ ] "Delete everything" leaves the app in a fresh-install state (instrumented test)
