# f061 2.0 release hardening: beta track, device matrix, release checklist
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P0 · **Size:** M · **Area:** Release

**Title:** Ship 2.0 through a staged beta with a defined device test matrix

**Description:**
2.0 changes the DB schema several times, the engine lifecycle, downloads and the tool system. It shouldn't go straight from `dev` to production.

**Plan:**
- **Feature freeze:** start of M4 (target 2027-03-08); only bug fixes after
- **Closed testing track** on Play for ≥ 2 weeks; GitHub pre-release APKs for sideload users
- **Device matrix** (minimum): one Snapdragon 8-series (NPU path, #40), one Exynos/Tensor, one MediaTek, one 6 GB RAM device, one 12 GB+ device, plus the oldest supported Android version (f003)
- **Upgrade test:** install 1.1.3 → create data in every feature → upgrade to 2.0 → verify everything survived
- `docs/play-release-checklist.md` extended with the 2.0 items (data safety form: diagnostics f010, backup f055, notification digest f044)

**Acceptance Criteria:**
- [ ] Zero open P0 issues in the `2.0` milestone
- [ ] Crash-free sessions in beta ≥ 99% (from f010 reports)
- [ ] 1.1.3 → 2.0 upgrade verified on 3 devices with no data loss
- [ ] Release notes drafted and the What's new sheet (f054) final
- [ ] Tag `v2.0.0` builds a signed AAB via the existing workflow
