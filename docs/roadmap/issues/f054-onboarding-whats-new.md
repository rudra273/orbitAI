# f054 Onboarding 2.0 and a "What's new in 2.0" screen
Labels: enhancement
**Milestone:** M4 Hardening & Beta · **Priority:** P2 · **Size:** M · **Area:** Onboarding
**Depends on:** f017, f018

**Status:** 🟡 Done, device check pending (2026-10-04). After the welcome screen, new users get a Setup screen: the model that suits their phone with a one-tap background download (progress, token hint if the model is gated, "choose a different model"), plus five tour cards (Spaces, Modes, Tools, Bubble, text selection). Skippable, and reopenable from Settings > Setup and tour. A "What's new in 2.0" dialog is shown once to existing users. Left out on purpose: the Gemini cloud option (privacy first) and a download progress pill in the chat screen (the download keeps running in the background and shows in Settings > Models).

**Title:** Get new users to a working model in under a minute, and show upgraders what changed

**Description:**
`WelcomeScreen` collects the user's name and explains the app, but a new user still has to find Settings → Models, choose among 13 models, and wait for a multi-GB download before chatting. Many will leave before their first answer.

**Proposed behavior:**
- Step: "Pick your brain": a device-aware recommended model (f017) with a one-tap background download (f018). Users can explore the app meanwhile, with a progress pill in the chat screen
- Optional step: Gemini as a cloud option while the local model downloads (with the existing cloud disclosure)
- Short feature tour cards: Spaces, Modes, Tools, Bubble, text-selection actions (f042)
- **What's new** sheet shown once after updating to 2.0, listing the headline features

**Acceptance Criteria:**
- [ ] Fresh install → first answer possible without visiting Settings
- [ ] Onboarding is skippable and re-openable from Settings → About
- [ ] What's new appears once per major version
