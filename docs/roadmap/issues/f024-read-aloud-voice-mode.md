# f024 Read replies aloud and a hands-free voice conversation mode
Labels: enhancement
**Milestone:** M2 Core Experience · **Priority:** P1 · **Size:** L · **Area:** Voice

**Title:** TTS for assistant messages and a continuous talk → listen → answer → speak loop

**Description:**
Voice input exists (`VoiceInput.kt`, the mic button), and `TextToSpeech` is already initialised, but **only** for the call-confirmation flow. Users can't have an answer read to them, and there's no hands-free conversation (cooking, driving, accessibility).

**Proposed behavior:**
- **Read aloud** action on assistant messages (f022) with play/pause/stop; strip markdown and code before speaking
- **Voice mode** (headset/mic icon): full-screen minimal UI with a waveform. Orbit listens, sends, streams the answer, speaks it sentence by sentence *while generating*, then listens again
- Interrupt by tapping, or by speaking (barge-in) where the recognizer allows it
- Settings: voice, speech rate, "Auto-read replies" toggle
- Prefer on-device recognition (`EXTRA_PREFER_OFFLINE`); keep the existing disclosure that the device speech provider may be cloud-based

**Acceptance Criteria:**
- [x] First sentence is spoken before generation finishes (sentence-chunked TTS queue)
- [x] Voice mode survives screen-off with a foreground notification, or explicitly stops (decide in PR)
- [x] Existing call voice flow (`handleVoiceCall`) keeps working inside voice mode
- [x] TalkBack users can start/stop voice mode with one action

**Status:** 🟡 Done, device check pending (2026-10-04). Read aloud button on replies (play/stop) and Settings > Voice (auto-read, speed, voice with sample). Voice mode (sound-wave button when the input is empty): listen → send → speak sentence by sentence while the reply streams (SpeechText.Stream) → listen; tap the orb to interrupt. Screen-off decision: voice mode keeps the screen on and ends when the app goes to the background (no foreground service). Call requests go through the existing handleVoiceCall confirmation inside voice mode. No barge-in while speaking (avoids the speaker being heard as speech). Device check pending.
