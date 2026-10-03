# f017 Device-aware model recommendations and a RAM guard
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** M · **Area:** Models / Onboarding

**Title:** Recommend models that fit the phone and warn before downloading ones that won't run

**Description:**
The catalog ranges from Gemma 3 270M (~0.9 GB) to Phi-4 14B (~10.9 GB). Nothing checks device RAM or free storage, so a user can spend an hour downloading a model that is then killed by the low-memory killer on first load. New users also don't know which model to pick.

**Proposed behavior:**
- Add `minRamGb` and `downloadBytes` (from `docs/model-catalog-audit.md`) to each catalog entry
- Read `ActivityManager.MemoryInfo.totalMem` and free space in `getExternalFilesDir(null)`
- Model screen sections: **Recommended for this phone**, **May be slow**, **Not supported on this device** (download still allowed behind a confirmation)
- Block a download when free storage < download size + 10%, with a clear message
- Show a "Best for: quick replies / reasoning / images" tag

**Acceptance Criteria:**
- [x] 6 GB, 8 GB and 12 GB device profiles each get a sensible recommended model (unit test on the recommendation function)
- [x] Storage check prevents starting a download that can't finish
- [x] Warning shown before loading a model whose `minRamGb` exceeds device RAM
- [ ] Onboarding (f054) uses the same recommendation

**Status:** ✅ Done (2026-10-03). New `core/model/DeviceFit` (pure): RAM need = download size × 1.3 + 1 GB; Fits (≤60% of total RAM), May be slow (≤85%) or Too large. Recommendation = first model that fits from [Gemma 4 E4B, Gemma 4 E2B, Gemma 3 1B LiteRT, Gemma 3 270M]: 12 GB → E4B, 8 GB → E2B, 6 GB → Gemma 3 1B. Storage check keeps a 10% margin. 7 unit tests. `DeviceSpecs` reads RAM (`ActivityManager.MemoryInfo`) and free space (`StatFs`). Models screen shows this phone's RAM/free space, puts the recommended model first with a 'RECOMMENDED FOR THIS PHONE' badge (replacing the hard-coded Gemma 4 E2B one), and every row shows size · best-for · coloured fit. Downloading a too-large model asks for confirmation; a download is refused with a clear message when storage is short (counting any partial file). Loading a too-large model in chat shows a warning. Sizes are computed from the pinned download sizes, so no per-model `minRamGb` field was needed. Open: onboarding use is part of f054.
