plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

import java.util.Properties
import java.net.URI

val playSupportEmail = providers.gradleProperty("orbitSupportEmail")
    .orElse(providers.environmentVariable("ORBIT_SUPPORT_EMAIL")).getOrElse("")
val playPrivacyPolicyUrl = providers.gradleProperty("orbitPrivacyPolicyUrl")
    .orElse(providers.environmentVariable("ORBIT_PRIVACY_POLICY_URL")).getOrElse("")
val playReportEndpoint = providers.gradleProperty("orbitReportEndpoint")
    .orElse(providers.environmentVariable("ORBIT_REPORT_ENDPOINT")).getOrElse("")

fun String.javaStringLiteral() = "\"" + replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

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
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.orbitai"
        minSdk = 35
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPPORT_EMAIL", playSupportEmail.javaStringLiteral())
        buildConfigField("String", "PRIVACY_POLICY_URL", playPrivacyPolicyUrl.javaStringLiteral())
        buildConfigField("String", "REPORT_ENDPOINT", playReportEndpoint.javaStringLiteral())
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
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("qa") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            versionNameSuffix = "-qa"
            buildConfigField("String", "REPORT_ENDPOINT", "".javaStringLiteral())
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
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val validatePlayReleaseConfiguration = tasks.register("validatePlayReleaseConfiguration") {
    doLast {
        check(playSupportEmail.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {
            "Set orbitSupportEmail or ORBIT_SUPPORT_EMAIL to the real developer contact email."
        }
        for ((name, value) in listOf("orbitPrivacyPolicyUrl" to playPrivacyPolicyUrl, "orbitReportEndpoint" to playReportEndpoint)) {
            val uri = runCatching { URI(value) }.getOrNull()
            check(uri?.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) {
                "Set $name to a real, publicly reachable HTTPS URL before building a Play release."
            }
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validatePlayReleaseConfiguration)
}

val validateQaSigning = tasks.register("validateQaSigning") {
    doLast {
        check(android.buildTypes.getByName("qa").signingConfig != null) {
            "Set the upload key and its credentials in keystore.properties before building a signed QA bundle."
        }
    }
}
tasks.matching { it.name == "preQaBuild" }.configureEach {
    dependsOn(validateQaSigning)
}
// LiteRT-LM 0.20+ ships Java 25 class files, so JVM unit tests that load it need a Java 25 runtime.
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.genai:google-genai:2.6.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    implementation(libs.androidx.appcompat)
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

    implementation(libs.mediapipe.llm.inference)
    implementation(libs.litertlm.android)
    implementation(libs.onnxruntime.android)
    implementation(files("libs/onnxruntime-genai-android-0.13.1.aar"))
    implementation(libs.mediapipe.text)
    implementation(libs.pdfbox.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.work.runtime.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
