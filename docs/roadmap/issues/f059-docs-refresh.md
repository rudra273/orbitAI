# f059 Documentation refresh for 2.0
Labels: documentation, good first issue
**Milestone:** M4 Hardening & Beta · **Priority:** P2 · **Size:** S · **Area:** Docs

**Title:** Bring README, CLAUDE.md and in-repo docs up to date

**Description:**
- README "Screenshots" is a placeholder (*"Add screenshots here…"*); the feature list predates ONNX, Gemma 4, notification replies, calls and the reminder manager
- CLAUDE.md says the DB is "currently **version 11**"; it's **13** (and will move again in 2.0)
- No user guide for Spaces, Modes, Tools or the bubble beyond README sections
- No CONTRIBUTING notes (branching `dev` → `main`, issue numbering `fNNN`, the test/lint commands from f004)

**Acceptance Criteria:**
- [ ] README: current screenshots (light + dark), 2.0 feature list, supported devices (f003)
- [ ] CLAUDE.md: DB version, app container (f007), tool registry (f040), schema-export rule (f005)
- [ ] `docs/user-guide.md` covering every user-facing feature
- [ ] `CONTRIBUTING.md` with the workflow and checks
