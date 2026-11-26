# f044 Daily notification digest (opt-in, on-device)
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** L · **Area:** Automation / Privacy

**Status:** 🟡 Built, device check pending (2026-10-04). Settings > Tools > Notification digest: off by default with its own disclosure, only WhatsApp, WhatsApp Business, Signal, Telegram, Google Messages and Gmail can be chosen (no banking or payment apps), title and text are held in memory for up to 24 hours (never on disk), one-time codes, passwords and bank or payment messages are dropped before they are kept, and the summary is written only when the user taps Make digest, by a model on the phone. Not built, on purpose: a daily scheduled digest (a background model run adds Play and battery risk). Play review is still the open risk, see the note below; to remove the feature, delete `feature/automation/digest`, `DigestScreen`, the hook in `NotificationReplyListener` and the Tools link.

**Play policy check (added 2026-10-04):** this reads other apps' notifications, which Google Play reviews closely (sensitive user data, prominent in-app disclosure before access, Data safety form, a purpose that is core to the app). Before building: confirm the existing Message replies feature passes review with the same listener, keep the digest off by default with its own disclosure, never store OTPs or one-time codes (filter them out before anything is buffered), and keep the buffer in memory or encrypted and cleared after each digest. If review pushes back, remove the digest and keep Message replies. Do not ship it unless this holds.

**Title:** Summarize the day's notifications from selected apps with the local model

**Description:**
`NotificationReplyListener` already has notification-listener access for selected apps (Message replies feature) and keeps previews only in memory. Users with notification overload would value a periodic digest: "3 people asked about Saturday; your bank sent an OTP; 2 delivery updates".

**Proposed behavior:**
- Settings → Tools → Notification digest (separate opt-in and disclosure; reuses the existing listener and app selection)
- Collect title + text from selected apps into an encrypted, size-capped local buffer cleared after each digest
- At the chosen time(s), WorkManager runs the local model **only** (never cloud) and posts one notification with the digest; tap opens it as a temporary chat (f027)
- Exclude OTPs and sensitive categories by default (heuristic + user filter)

**Acceptance Criteria:**
- [ ] No notification content persists beyond the digest window
- [ ] Digest never runs on Gemini
- [ ] Battery: digest job runs only when charging or battery > 30% (configurable)
- [ ] Privacy policy updated
