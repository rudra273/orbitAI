# f010 Privacy-respecting crash and ANR diagnostics
Labels: enhancement
**Milestone:** M1 Foundation · **Priority:** P1 · **Size:** M · **Area:** Quality

**Title:** Add StrictMode in debug and an opt-in local crash log that users can send

**Description:**
There is no crash visibility today: no crash reporter, no StrictMode, no ANR capture. On-device inference crashes are device-specific (GPU drivers, NPU, low memory), so without data we can't fix what users hit. OrbitAI's privacy positioning rules out a silent third-party SDK by default.

**Proposed behavior:**
- Debug builds: `StrictMode` thread and VM policies with `penaltyLog()`
- All builds: an uncaught-exception handler writes the stack trace, app version, device model, Android version, active model ID and active backend (no chat content) to a rotating local file
- Settings → About → "Send diagnostics": user previews the log, then shares it via email intent to `BuildConfig.SUPPORT_EMAIL`
- On next launch after a crash: a one-time, dismissible "Orbit closed unexpectedly. Send report?" prompt
- Use `ApplicationExitInfo` to record low-memory kills and ANRs from the previous session

**Acceptance Criteria:**
- [ ] No chat text, memory, document content, tokens or API keys are written to the log (unit test on the log formatter)
- [ ] User sees the full report before sending
- [ ] Low-memory kills during model load are recorded with model ID and size
- [ ] Privacy policy screen updated to describe diagnostics
