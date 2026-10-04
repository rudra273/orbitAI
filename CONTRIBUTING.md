# Contributing

Pull requests are welcome. Open an issue first for larger changes.

## Workflow

- Work happens on `dev`; `main` is for releases. Open pull requests against `dev`.
- Roadmap items are numbered `fNNN` in `docs/roadmap/issues/`; mention the number in the commit message and update the item's `**Status:**` line and the table in `docs/roadmap/v2.0-plan.md` when you finish it.
- Keep changes to what the issue asks. No unrequested refactors or new dependencies; if a dependency version conflicts, report the exact error instead of working around it.

## Checks

Set `JAVA_HOME` to a JDK 17+ (Android Studio's bundled one works), then:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Lint reports a handful of warnings that are already in `app/lint-baseline.xml`; a new warning should be fixed rather than added to the baseline.

## Database changes

- Bump `AppDatabase.VERSION` and add a hand-written `Migration` to `ALL_MIGRATIONS`. There are no auto-migrations.
- Commit the new schema JSON in `app/schemas/` and extend `app/src/androidTest/.../database/MigrationTest.kt`.
- Run the instrumented tests on a device: `./gradlew installDebugAndroidTest`, then
  `adb shell am instrument -w com.example.orbitai.test/androidx.test.runner.AndroidJUnitRunner`.

## Code conventions

- `core/*` is infrastructure (engine, model, prompt, database, common); `feature/*` is user-facing features; UI lives in `ui/screens`, `ui/navigation`, `ui/theme`.
- Shared objects come from `context.appContainer`; do not build repositories or the `LlmRepository` yourself.
- Wrap inference in `LlmRepository.beginUse()` / `endUse()` and free memory with `releaseIfIdle()`.
- Regexes that run on user text need `\p{M}` next to `\p{L}` so Hindi and other combining scripts work.
- Settings stores are small `SharedPreferences` wrappers; use the `edit { }` KTX form.
