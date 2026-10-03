# f056 App lock and an optional encrypted database
Labels: enhancement, question
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** L · **Area:** Privacy / Security

**Title:** Protect chats and memories with biometric lock and encryption at rest

**Description:**
OrbitAI stores highly personal data in plaintext SQLite (`orbitai.db`): chats, memories ("my bike is parked…", health questions), Space documents and reminders. Anyone with an unlocked phone can open the app. API keys are in SharedPreferences (`TokenStore`).

**Proposed behavior:**
- **App lock:** `androidx.biometric` `BiometricPrompt` (biometric or device credential) on launch and after N minutes in background; also hides content in the recents thumbnail (`FLAG_SECURE` option)
- **Secrets:** move the HF token and Gemini API key to Android Keystore-backed encrypted storage
- **Database encryption (decision needed):** SQLCipher for Room adds a native dependency and APK size, with a one-time migration that re-encrypts the existing DB. The owner decides whether 2.0 ships it or only app lock + secrets

**Acceptance Criteria:**
- [ ] App lock cannot be bypassed via widgets, shortcuts, share intents or the bubble's "open app" (each routes through the lock)
- [ ] Existing tokens migrated to encrypted storage, old prefs keys wiped
- [ ] If DB encryption ships: migration tested on a DB with 1k+ messages; failure path never deletes data
