# f018 Resumable background model downloads with integrity checks
Labels: enhancement, bug
**Milestone:** M1 Foundation · **Priority:** P0 · **Size:** L · **Area:** Models

**Title:** Move model downloads to WorkManager with HTTP Range resume, a progress notification and SHA-256 verification

**Description:**
Models are 0.6–11 GB, but downloads run in `DownloadViewModel`'s `viewModelScope`. They stop when the ViewModel is cleared (leaving the screen, process death, the OS reclaiming a backgrounded app). `ModelDownloader` writes to `<file>.tmp` and **deletes the temp file on any failure**, so a dropped connection at 95% of a 4 GB file restarts from zero. `DownloadStatus.PAUSED` exists but no resume path uses it. There's no integrity check, so a truncated file can be renamed into place and then fail at load time with an opaque native error.

**Proposed behavior:**
- A `ModelDownloadWorker` (WorkManager, foreground with `dataSync` type) per model, with notification progress + Cancel action
- Keep partial `.tmp` files; resume with `Range: bytes=<existing>-` and handle `206` vs `200`
- Pause / resume buttons in the Model screen
- Store expected `sha256` per file in `ModelDownloads.kt` (from the pinned HF revisions in `docs/model-catalog-audit.md`) and verify before the final rename
- Respect "Wi-Fi only" (setting, default on for > 1 GB)

**Acceptance Criteria:**
- [ ] Download continues with the app closed and survives process death
- [ ] Airplane mode on/off mid-download resumes from the last byte, not zero
- [x] Corrupted or truncated files are rejected with "Download corrupted, retry"
- [x] Multi-file ONNX models resume per file
- [x] HF token header still sent for gated repos
- [x] Manifest declares the needed foreground-service type and permission

**Status:** 🟡 Done, pending device check (2026-10-03). Catalog downloads run in `ModelDownloadWorker` (WorkManager 2.12.0, foreground `dataSync`, progress notification with Pause). Partial `.tmp` files are kept on interruption and resumed with `Range` (206 append / 200 restart / 416 reset); resumable failures auto-retry with backoff. Each file is verified by exact size, plus SHA-256 for 8 of 13 models (`ModelChecksums.kt`, taken from HF metadata of the pinned revisions). Models screen: Pause / Resume / Cancel (Cancel discards partial data), 'Verifying' and 'Waiting for Wi-Fi…' states, plus a 'large models on Wi-Fi only' toggle (default on, >1 GB). Logic is in the context-free `ResumableFileDownloader` with 11 MockWebServer tests. Remaining: the 5 gated Gemma repos lack checksums (need an HF token, see docs/model-catalog-audit.md); embedding and custom-URL downloads still run in the ViewModel, though they share the resume logic. ⚠️ Background/process-death and airplane-mode behaviour need a device run.
