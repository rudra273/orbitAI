plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

import java.util.Properties

fun parseReleaseVersionName(): String {
    val rawVersion = System.getenv("ORBIT_RELEASE_VERSION")
        ?: (findProperty("orbitReleaseVersion") as String?)
        ?: "1.0.0"
    return rawVersion.removePrefix("v")
}

fun parseReleaseVersionCode(versionName: String): Int {
    val explicitVersionCode = System.getenv("ORBIT_RELEASE_CODE")
        ?: (findProperty("orbitReleaseCode") as String?)
    explicitVersionCode?.toIntOrNull()?.let { return it }

    val numericPart = versionName.substringBefore('-')
    val parts = numericPart.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.isEmpty()) return 1

    val major = parts.getOrElse(0) { 0 }.coerceAtLeast(0)
    val minor = parts.getOrElse(1) { 0 }.coerceIn(0, 99)
    val patch = parts.getOrElse(2) { 0 }.coerceIn(0, 99)
    return (major * 10_000) + (minor * 100) + patch
}

android {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.isFile) {
            keystorePropertiesFile.inputStream().use(::load)
        }
    }

    val signingStoreFilePath = keystoreProperties.getProperty("storeFile")
    val signingStorePassword = keystoreProperties.getProperty("storePassword")
    val signingKeyAlias = keystoreProperties.getProperty("keyAlias")
    val signingKeyPassword = keystoreProperties.getProperty("keyPassword")
    val hasReleaseSigning = !signingStoreFilePath.isNullOrBlank() &&
        !signingStorePassword.isNullOrBlank() &&
        !signingKeyAlias.isNullOrBlank() &&
        !signingKeyPassword.isNullOrBlank()

    namespace = "com.example.orbitai"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        val releaseVersionName = parseReleaseVersionName()
        applicationId = "com.example.orbitai"
        minSdk = 35
        targetSdk = 36
        versionCode = parseReleaseVersionCode(releaseVersionName)
        versionName = releaseVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(signingStoreFilePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ✅ AGP 9.x — replaces kotlinOptions
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE"
        }
        jniLibs { pickFirsts += "**/*.so" }
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.genai:google-genai:1.44.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    implementation(libs.mediapipe.llm.inference)
    implementation(libs.litertlm.android)
    implementation(libs.onnxruntime.android)
    implementation(files("libs/onnxruntime-genai-android-0.13.1.aar"))
    implementation(libs.mediapipe.text)
    implementation(libs.pdfbox.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
