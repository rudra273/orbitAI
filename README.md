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
