./gradlew installDebug && adb shell monkey -p com.example.orbitai -c android.intent.category.LAUNCHER 1

adb logcat | grep orbitai