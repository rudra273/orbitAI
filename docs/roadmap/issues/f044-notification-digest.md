# f044 Daily notification digest (opt-in, on-device)
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** L · **Area:** Automation / Privacy

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
