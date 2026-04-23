# OrbitAI

OrbitAI is an advanced on-device AI chat and productivity assistant for Android, built with Jetpack Compose and modern Kotlin. It leverages local LLM (Large Language Model) inference, RAG (Retrieval-Augmented Generation), and a suite of productivity tools—all running privately on your device.

## Features

- **On-device LLM Chat**: Private, fast, and offline-capable AI chat using MediaPipe and LiteRtLm engines.
- **Retrieval-Augmented Generation (RAG)**: Enhanced responses by embedding and searching your own data.
- **Productivity Tools**: Floating bubble assistant, reminders, and more.
- **Multiple Modes**: Switch between chat, spaces (knowledge bases), and custom modes.
- **Modern UI**: Built with Jetpack Compose for a smooth, responsive experience.
- **No Cloud Required**: All AI runs locally—your data stays on your device.

## Screenshots

*Add screenshots here to showcase the UI and features.*

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
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

Build the signed Play artifact locally:

```sh
./gradlew bundleRelease
```

Upload the generated `.aab` from `app/build/outputs/bundle/release/` to Play Console.

For signed GitHub releases, add these repository secrets:

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
- `INTERNET`, `RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `READ/WRITE_EXTERNAL_STORAGE`, `READ_CONTACTS`, `POST_NOTIFICATIONS`

## Contributing
Pull requests are welcome! Please open an issue first to discuss major changes.

## License
*Specify your license here.*

---
*This project is not affiliated with Google or any LLM provider. All trademarks are property of their respective owners.*
