./gradlew installDebug && adb shell monkey -p com.orbitai -c android.intent.category.LAUNCHER 1

adb logcat | grep orbitai
