# OrbitAI

OrbitAI is an advanced on-device AI chat and productivity assistant for Android, built with Jetpack Compose and modern Kotlin. It leverages local LLM (Large Language Model) inference, RAG (Retrieval-Augmented Generation), and a suite of productivity tools—local models running on your device, with an optional Gemini cloud provider.

## Features

- **On-device LLM Chat**: Private, fast, and offline-capable AI chat using MediaPipe and LiteRtLm engines.
- **Retrieval-Augmented Generation (RAG)**: Enhanced responses by embedding and searching your own data.
- **Productivity Tools**: Floating bubble assistant, reminders, and more.
- **Multiple Modes**: Switch between chat, spaces (knowledge bases), and custom modes.
- **Modern UI**: Built with Jetpack Compose for a smooth, responsive experience.
- **Optional Cloud AI**: Local inference stays on your device. Gemini sends prompts, relevant context and attachments to Google only after confirmation. Voice recognition may use your device's speech provider when on-device recognition is unavailable.

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
- **MediaPipe Tasks, LiteRtLm** for on-device LLM inference.
- **Accompanist, Material3, Navigation Compose** for UI/UX.

## Permissions
- `INTERNET`, `RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `READ_CONTACTS`, `POST_NOTIFICATIONS`
- Accessibility, broad storage access and APK installation permissions are absent from the Play branch. Screen capture requests Android consent each time; generated text is copied for manual pasting.

## Contributing
Pull requests are welcome! Please open an issue first to discuss major changes.

## License
*Specify your license here.*

---
*This project is not affiliated with Google or any LLM provider. All trademarks are property of their respective owners.*
