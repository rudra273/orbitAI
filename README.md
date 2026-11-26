# OrbitAI

OrbitAI is an advanced on-device AI chat and productivity assistant for Android, built with Jetpack Compose and modern Kotlin. It leverages local LLM (Large Language Model) inference, RAG (Retrieval-Augmented Generation), and a suite of productivity tools—local models running on your device, with an optional Gemini cloud provider.

## Features

- **On-device chat**: private, offline-capable chat with LiteRT-LM, ONNX Runtime GenAI and MediaPipe `.task` models, a device-aware model recommendation, resumable background downloads, and your own `.litertlm` / `.task` files (Settings > Models > Import model file).
- **Spaces (RAG)**: add PDF, Word (.docx), EPUB and text files; answers use hybrid keyword + meaning search and show the pages they came from.
- **Memory**: Orbit suggests facts to remember and you tap to save; only relevant ones are used in a reply.
- **Tools by natural language**: WhatsApp, email, SMS, calls, reminders (repeating too), timers, alarms, navigation, open app, web search, calendar events and flashlight, with typo-tolerant commands, native tool calling on LiteRT-LM models, and a confirmation before every action. Each tool can be switched off in Settings > Tools.
- **Orbit Bubble**: floating assistant with hands-free voice, voice follow-ups, and Continue in app.
- **Text selection and sharing**: select text anywhere and choose Orbit to proofread, rewrite, translate (Hindi by default, any language works), summarise, explain or reply; share files and images to Orbit.
- **Organise and export**: search all chats, pin, rename, archive, and export a chat as PDF, Markdown or text.
- **Modes**: reusable assistants with their own instructions, model, Spaces and colour; templates, import and export.
- **Your data**: encrypted backup and restore, a Privacy center (storage, 30-day network log, permissions, delete everything), and light/dark/system themes with chat text size.
- **Optional Cloud AI**: local inference stays on your device. Gemini sends prompts, relevant context and attachments to Google only after confirmation. Voice recognition may use your device's speech provider when on-device recognition is unavailable.

See the [user guide](docs/user-guide.md) for how each feature works and [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow.

## Orbit reminders, calls, and memory

- **Reminders:** ask “Remind me at 10 pm to call Mom”, “Remind me in 10 minutes to drink water”,
  or “Set a reminder every Friday at 6 pm to submit expenses”. These common commands work
  without a model. Orbit uses the phone's local time zone and shows an editable preview;
  **10 pm means 22:00**. Without a date, an elapsed time moves to the next day. Check suggested
  fields for unsupported or incomplete phrases before saving.
- **Own reminder manager:** open **Settings > Tools > Saved reminders** to create reminders
  directly, edit them, set daily/weekly repeats, snooze for 10 minutes, complete, or delete.
  Reminder notifications also have Snooze and Done actions. Done dismisses the current
  occurrence of a repeating reminder; Stop repeating in the manager ends the series.
- **Delivery:** allow notifications and the OrbitAI Reminders channel. Enable Android
  **Alarms & reminders** access from the manager for precise alarms; otherwise delivery
  is approximate. Saved reminders are restored on reboot, app update, and reopening Orbit.
  Android force-stop and device power-off can still prevent delivery until the app/device resumes.
- **Calendar:** remains an explicit alternative in the preview; reminder commands default
  to saving in Orbit, regardless of the old calendar-handoff preference.
- **Sharing:** share text to Orbit and choose Create reminder, then send the prefilled command
  and review the result. No overlay or microphone access is needed.
- **Calls:** ask “Call Mom” or “Call +91 98765 43210”. Contact lookup ranks saved names by
  spelling and common pronunciation variants. In chat voice input, finish saying the request:
  Orbit speaks the closest match, then listens for “yes” to call or “no” to cancel. With several
  close matches, say the name or option number, then confirm the selected contact. Typed requests
  show a contact review. Contacts and direct calls need Android permission; the first permission
  grant still requires Android's system dialog. Say “open dialer” after a denied phone permission
  to continue manually. Short phone numbers use the dialer. No model or cloud AI is needed.
- **Memory:** “Remember that my bike is parked on level 2” saves to the existing Orbit Memory
  section, without a model or cloud request. If Memory is disabled, Orbit reports that nothing
  was saved there. Manage entries in Settings > Memory.

Alarms from versions before reminder persistence cannot be recovered after reboot because
those versions did not store them.

## Optional notification replies

Open **Settings > Tools > Message replies**, enable the feature after its disclosure,
select apps, and grant Android notification access. Only active notifications with one
usable text reply action appear. Support depends on the messaging app's notification
format; selecting an app does not guarantee every conversation can be replied to.

Choose **Review reply**, write a response or provide an instruction for **Draft with local AI**,
review the conversation and wording, then tap **Send reply**. A downloaded local model is
required for AI drafts, even if Gemini is your usual chat model. Previews and drafts stay
in memory, outside chat history. Orbit checks for changed or removed notifications before
handing the reply to the originating app; delivery is not confirmed by Orbit.

This version does not send unattended auto-replies. Disable the feature to clear previews;
Android notification access can also be revoked from the same screen.

## Screenshots

*Add screenshots here to showcase the UI and features.*

## Getting Started

### Prerequisites
- Android Studio compatible with the project's Android Gradle Plugin version (currently 9.1.0)
- Android device or emulator (minSdk 35, targetSdk 36)
- [Download or build compatible LLM and embedding models](#models)

### Build & Run

```sh
./gradlew installDebug && adb shell monkey -p com.example.orbitai -c android.intent.category.LAUNCHER 1
```

To view logs:
```sh
adb logcat | grep orbitai
```

Run the same checks CI runs on every pull request:
```sh
./gradlew testDebugUnitTest lintDebug
```
Existing lint findings are recorded in `app/lint-baseline.xml`; new lint errors fail the check.

### Play Store Upload Key

Create or reuse one private upload key before the first Play Console upload:

```sh
mkdir -p ~/android-signing

keytool -genkeypair \
  -v \
  -keystore ~/android-signing/upload-key.p12 \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -alias upload-key \
  -dname "CN=Upload Key, OU=Android, O=Private, L=Unknown, ST=Unknown, C=IN"
```

Create `keystore.properties` locally:

```properties
storeFile=/Users/rudrapratapmohanty/android-signing/upload-key.p12
storePassword=your_keystore_password
keyAlias=upload-key
keyPassword=your_key_password
```

Both `upload-key.p12` and `keystore.properties` are ignored by git. Keep a private backup of the keystore and passwords. You can reuse this same upload key for other apps if you also register its certificate as the upload key in each app's Play Console setup.

Print the certificate fingerprints:

```sh
keytool -list -v -keystore ~/android-signing/upload-key.p12 -alias upload-key
```

Use the SHA-1/SHA-256 fingerprints in Play Console to confirm this key is registered as the **upload key**, while Google Play manages the **app signing key**.

Before building a release, configure real contact details and production HTTPS URLs in your local Gradle properties or environment:

```properties
orbitSupportEmail=YOUR_REAL_SUPPORT_EMAIL
orbitPrivacyPolicyUrl=YOUR_PUBLISHED_HTTPS_PRIVACY_POLICY_URL
orbitReportEndpoint=YOUR_PRODUCTION_HTTPS_REPORT_ENDPOINT
```

These are configuration placeholders in this documentation, not working destinations. Release builds refuse missing or invalid configuration. The equivalent environment variables are `ORBIT_SUPPORT_EMAIL`, `ORBIT_PRIVACY_POLICY_URL` and `ORBIT_REPORT_ENDPOINT`. Debug builds work without them, but reports cannot be sent until the endpoint is configured.

See [Play release checklist](docs/play-release-checklist.md) for the report endpoint contract, required policy publication and Play Console steps.

Build the signed Play artifact locally:

```sh
./gradlew bundleRelease
```

Upload the generated `.aab` from `app/build/outputs/bundle/release/` to Play Console.

For GitHub release builds, also set repository variables `ORBIT_SUPPORT_EMAIL`, `ORBIT_PRIVACY_POLICY_URL` and `ORBIT_REPORT_ENDPOINT`. Add these repository secrets for signing:

```text
ANDROID_UPLOAD_KEYSTORE_BASE64
ANDROID_UPLOAD_KEYSTORE_PASSWORD
ANDROID_UPLOAD_KEY_ALIAS
ANDROID_UPLOAD_KEY_PASSWORD
```

Copy the keystore into `ANDROID_UPLOAD_KEYSTORE_BASE64` on macOS:

```sh
base64 -i ~/android-signing/upload-key.p12 | tr -d '\n' | pbcopy
```

Set `ANDROID_UPLOAD_KEY_ALIAS` to `upload-key`. Pushing a tag such as `v1.0.0` will build and upload signed release artifacts.

### Models
- Place your LLM and embedding models in the app's external files directory under `models/`.
- Example: `universal_sentence_encoder.tflite` for text embedding.

## Architecture
- **Kotlin, Jetpack Compose, Room** for UI and data.
- **LiteRT-LM, ONNX Runtime GenAI, MediaPipe Tasks** for on-device LLM inference, behind one shared `LlmRepository`.
- **WorkManager** for document indexing and model downloads; **PDFBox** for PDFs.
- **Accompanist, Material3, Navigation Compose** for UI/UX.

## Permissions
- `INTERNET`, `RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `READ_CONTACTS`, `CALL_PHONE`, `POST_NOTIFICATIONS`, `SET_ALARM`, `SCHEDULE_EXACT_ALARM`
- Accessibility, broad storage access and APK installation permissions are absent from the Play branch. Screen capture requests Android consent each time; generated text is copied for manual pasting.

## Contributing
Pull requests are welcome! Please open an issue first to discuss major changes.

## License
*Specify your license here.*

---
*This project is not affiliated with Google or any LLM provider. All trademarks are property of their respective owners.*
