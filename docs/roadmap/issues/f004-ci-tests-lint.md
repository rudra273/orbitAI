# f004 Run unit tests and Android lint in CI on every PR
Labels: enhancement, good first issue
**Milestone:** M1 Foundation · **Priority:** P0 · **Size:** S · **Area:** Quality

**Title:** Add `./gradlew testDebugUnitTest lintDebug` to the PR workflow

**Description:**
`.github/workflows/build-release.yml` only runs `assembleRelease` on PRs. The unit suites under `app/src/test/` (router, draft parsers, prompt builders, reminder parser, contact matcher) never run in CI, so a regression in parsing or prompt logic can merge silently. With ~60 roadmap items landing over six months, CI has to catch this.

**Proposed behavior:**
- A `test` job runs `./gradlew testDebugUnitTest` on every PR to `main`, `dev` and `play`
- A `lint` job runs `./gradlew lintDebug` and uploads the HTML report as an artifact
- The release build job depends on `test` passing

**Acceptance Criteria:**
- [x] PRs to `main`, `dev` and `play` run unit tests; a failing test fails the check
- [x] Lint report uploaded as a workflow artifact
- [x] Lint baseline (`lint-baseline.xml`) committed so existing warnings don't block, and new errors do
- [x] Gradle caching enabled (already via `gradle/actions/setup-gradle`)
- [x] README mentions how to run the same checks locally

**Status:** ✅ Done (2026-10-03). CI `check` job runs `testDebugUnitTest` + `lintDebug` on PRs to main/dev/play and uploads reports; release build now `needs: check`. Lint baseline `app/lint-baseline.xml` (108 existing findings; the only error is inside the BouncyCastle dependency jar). README documents the local command.
