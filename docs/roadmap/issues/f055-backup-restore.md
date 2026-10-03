# f055 Backup and restore all OrbitAI data
Labels: enhancement
**Milestone:** M3 Power Features · **Priority:** P1 · **Size:** L · **Area:** Data / Privacy
**Depends on:** f005

**Title:** Encrypted, user-controlled export/import of chats, memories, modes, Spaces and settings

**Description:**
The manifest sets `android:allowBackup="false"` (a sensible privacy default), so a new phone or a reinstall **loses everything**: chats, memories, custom modes, Space documents and reminders. Users who invest in Memory and Spaces need a way to move their data.

**Proposed behavior:**
- Settings → Backup & restore → **Create backup**: a single `.orbitbackup` file (zip of a DB snapshot + Space source files + settings JSON), encrypted with a user passphrase (AES-GCM, PBKDF2/Argon2 key derivation), saved via SAF
- **Restore:** pick the file, enter the passphrase, preview counts (chats, memories, modes, spaces), then replace or merge
- Models aren't included (too large); after restore, prompt to re-download the models used
- Optional auto-backup to a user-chosen folder weekly (WorkManager)

**Acceptance Criteria:**
- [ ] Restoring a backup from schema version N into an app at version N+k runs the migrations (tested)
- [ ] Wrong passphrase → clear error, no partial restore
- [ ] HF token and Gemini API key are **excluded** by default (opt-in include)
- [ ] Round-trip instrumented test: backup → wipe → restore → identical data
