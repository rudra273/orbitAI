# f003 Decide the minimum Android version for 2.0 (currently Android 15+ only)
Labels: enhancement, question
**Milestone:** M1 Foundation · **Priority:** P0 (decision) · **Size:** S to decide, M to implement · **Area:** Release

**Title:** Decide whether 2.0 lowers `minSdk` from 35 and audit API-35-only code

**Description:**
`app/build.gradle.kts` sets `minSdk = 35`, so OrbitAI installs only on Android 15 and newer. That excludes most of the active Android install base, including many phones that can run 1B–4B models (8 GB+ RAM devices on Android 12–14). Every later decision depends on this one: notifications, foreground-service types, the photo picker, and exact alarms behave differently on older APIs.

This needs an owner decision. Options:
1. Keep 35 (simplest, smallest test matrix).
2. Lower to 31 (Android 12): broad reach, and most APIs used here exist there.
3. Lower to 29/30: widest reach, more compatibility branches.

**Acceptance Criteria:**
- [x] Owner records the chosen `minSdk` in this issue
- [ ] ~~If lowered: Android lint `NewApi` passes with zero errors at the new `minSdk`~~ (not applicable)
- [ ] ~~If lowered: every API-gated call (`ImageDecoder`, `ServiceInfo.FOREGROUND_SERVICE_TYPE_*`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `MediaProjection` callbacks) is guarded with `Build.VERSION.SDK_INT` checks~~ (not applicable)
- [ ] ~~If lowered: an emulator smoke test on the new minimum API covers chat, model download, reminders and the bubble~~ (not applicable)
- [ ] ~~README "Prerequisites" updated~~ (not applicable)

**Status:** ✅ Done (2026-10-04). Owner decision (2026-10-04): keep `minSdk = 35` (Android 15+). No code change needed; the lowering-related items (NewApi audit, SDK_INT guards, old-API emulator test, README) don't apply.
