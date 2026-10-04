# f048 Spike: make Orbit selectable as the device's digital assistant
Labels: enhancement, question
**Milestone:** M3 Power Features · **Priority:** P2 · **Size:** M (spike) · **Area:** System integration

**Status:** 🟡 Desk research done (2026-10-04); the physical-device prototype is still open because it needs a phone.

**Findings (from Android's documented assistant APIs, not yet verified on a device):**
- **What makes an app selectable:** Settings > Default apps > Digital assistant lists apps that declare a `VoiceInteractionService` (bound with `BIND_VOICE_INTERACTION`) plus a `VoiceInteractionSessionService`, and a meta-data XML (`android:sessionService`, `android:recognitionService`, `android:supportsAssist="true"`). A plain activity with an `ACTION_ASSIST` filter is not enough on current Android. Orbit also needs a `RecognitionService` entry (the XML requires one); a thin wrapper around the platform recogniser is the usual approach.
- **Reusing UI:** the session's `onShow` can start `OrbitBubbleService` (already on the overlay permission) or open a small Compose activity; reusing the bubble keeps one voice path and the shared `LlmRepository`. The session itself should stay thin and hand off.
- **Screen context:** `onHandleAssist` / `onHandleScreenshot` give the assist structure and a screenshot when the user invokes the assistant and the system "use text/screenshot from screen" toggles are on. That would replace the MediaProjection prompt for "what's on my screen" questions on this path only; the bubble tap path would keep MediaProjection.
- **Play policy:** a voice interaction service must be user-selected, must not run in the background, and must disclose screen-context use in the privacy policy and Data safety form. Orbit's on-device-only handling helps.
- **Effort:** about M (2 to 3 days): service + session service + XML + recogniser wrapper + hand-off to the bubble + disclosure text.
- **Recommendation:** defer to 2.1 unless the owner wants assistant-gesture access in 2.0. It needs per-OEM device testing (Pixel, Samsung) that cannot be done without phones. If wanted, start with the prototype branch on one phone.

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
