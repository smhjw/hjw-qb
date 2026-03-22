import org.gradle.api.GradleException
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingProperty(name: String): String? {
    val fromFile = keystoreProperties.getProperty(name)?.trim().orEmpty()
    if (fromFile.isNotEmpty()) return fromFile
    return providers.gradleProperty(name).orNull?.trim()?.takeIf { it.isNotEmpty() }
}

val releaseStoreFilePath = signingProperty("RELEASE_STORE_FILE")
val releaseStorePassword = signingProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingProperty("RELEASE_KEY_PASSWORD")

val hasReleaseSigningConfig = !releaseStoreFilePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.hjw.qbremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hjw.qbremote"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.1.10"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningConfig) {
                storeFile = file(requireNotNull(releaseStoreFilePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            // Keep release builds behaviorally aligned with the verified debug builds.
            // The current app still has release-only runtime regressions under R8/resource shrinking.
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/libdatastore_shared_counter.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Newer AndroidX releases in lint currently require migrating the whole toolchain
        // to AGP 8.9.1+ and compileSdk 36. We intentionally pin these dependencies until
        // that broader upgrade is planned and validated end-to-end.
        disable += "GradleDependency"
    }
}

val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release", ignoreCase = true)
}
if (releaseTaskRequested && !hasReleaseSigningConfig) {
    throw GradleException(
        "Release signing config is missing. " +
            "Create keystore.properties from keystore.properties.example " +
            "or pass RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD."
    )
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("com.google.errorprone:error_prone_annotations:2.28.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
