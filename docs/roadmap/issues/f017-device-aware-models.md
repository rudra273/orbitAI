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
- [ ] 6 GB, 8 GB and 12 GB device profiles each get a sensible recommended model (unit test on the recommendation function)
- [ ] Storage check prevents starting a download that can't finish
- [ ] Warning shown before loading a model whose `minRamGb` exceeds device RAM
- [ ] Onboarding (f054) uses the same recommendation
