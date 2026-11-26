# f005 Export the Room schema and add migration tests
Labels: enhancement
**Milestone:** M1 Foundation · **Priority:** P0 · **Size:** M · **Area:** Data

**Title:** Turn on `exportSchema` and test every migration from v1 to the current version

**Description:**
`AppDatabase` is at **version 13**, with 12 hand-written migrations and `exportSchema = false`. No migration is tested. Several roadmap items need new schema changes: FTS search (f025), pinned/archived chats (f025), citations (f033), memory embeddings (f038), modes 2.0 (f039) and backup (f055). One bad migration on a user's device means a crash on launch or lost chat history. This is the highest data-loss risk in the 2.0 cycle.

**Proposed behavior:**
- Set `exportSchema = true` and configure the KSP `room.schemaLocation` to `app/schemas/`
- Commit the generated v13 schema JSON
- Add `androidx.room:room-testing` and a `MigrationTest` using `MigrationTestHelper`

**Acceptance Criteria:**
- [x] `app/schemas/com.example.orbitai.core.database.AppDatabase/13.json` committed
- [x] Instrumented test migrates a v13 DB with seeded chats, messages, spaces, memories, modes and reminders to the latest version and validates the data
- [x] Contributor rule added to CLAUDE.md: every schema bump commits the new schema JSON and adds a migration test case
- [x] CLAUDE.md updated (it still says "version 11")

**Status:** ✅ Done (2026-10-03). `exportSchema = true`, schema JSON in `app/schemas/`, `room-testing` added, migrations collected in `AppDatabase.ALL_MIGRATIONS` (used by both production and tests). `MigrationTest` builds a hand-made **v1** DB and runs the whole 1→13 chain with Room schema validation, plus a seeded v13→latest test via `MigrationTestHelper`. CLAUDE.md updated. Verified on SM-S928B (Android 16): 11/11 instrumented tests pass. Run without uninstalling the app: `./gradlew installDebugAndroidTest` then `adb shell am instrument -w com.example.orbitai.test/androidx.test.runner.AndroidJUnitRunner`.
