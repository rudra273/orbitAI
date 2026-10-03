# f048 Spike: make Orbit selectable as the device's digital assistant
Labels: enhancement, question
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M (spike) · **Area:** System integration

**Title:** Investigate letting users invoke Orbit with the assistant gesture (long-press power/home)

**Description:**
Users who want an on-device assistant want it on the system assistant gesture. Android exposes this through the **Default digital assistant app** setting, which normally requires a `VoiceInteractionService` (+ session service). A lighter `ACTION_ASSIST` activity may be enough on some devices or OEM skins.

This is a time-boxed investigation (≤ 3 days). Outcome: a short written recommendation in this issue.

**Questions to answer:**
- Minimal components needed for Orbit to appear in Settings → Default apps → Digital assistant app on Pixel and Samsung (One UI)
- Can the assist session reuse the bubble overlay UI (#39 split) or the voice mode (f024)?
- Does `onHandleAssist` give screen context (`AssistStructure`/screenshot) with user consent, so the MediaProjection prompt could be dropped for screen questions?
- Play policy implications

**Acceptance Criteria:**
- [ ] Prototype branch demonstrating invocation on at least one physical device
- [ ] Recommendation: ship in 2.0 / defer / drop, with effort estimate
