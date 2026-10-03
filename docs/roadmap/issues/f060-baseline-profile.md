# f060 Baseline profile and startup benchmarks
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P2 · **Size:** M · **Area:** Performance

**Title:** Faster cold start and smoother scrolling via a Baseline Profile

**Description:**
There's no `profileinstaller` and no baseline profile. Compose apps typically gain noticeably on cold start and first-scroll jank with one. Startup also does work worth measuring: `ChatViewModel.init` restores reminders, `ChatRepository`/`SpaceRepository` start eager collectors.

**Note:** generating profiles conventionally uses a separate `:baselineprofile` test module. That changes the single-module structure, so the owner confirms it first.

**Acceptance Criteria:**
- [ ] Baseline profile covering: launch → chat list → open chat → scroll → send message (fake engine)
- [ ] Macrobenchmark results (cold start, frame timing) before and after, in the PR
- [ ] Startup work audited: nothing heavier than reading prefs on the main thread before first frame
