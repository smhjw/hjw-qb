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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hjw.qbremote"
        minSdk = 26
        targetSdk = 36
        versionCode = 22
        versionName = "0.1.21"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            // R8 + resource shrinking: roughly halves APK size and restores
            // mapping.txt for Play crash deobfuscation. The Gson/Retrofit
            // reflection surface is pinned in proguard-rules.pro; if a release
            // build ever crashes at startup again, fix the specific keep rule
            // there instead of turning minification back off.
            isMinifyEnabled = true
            isShrinkResources = true
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
        // Dependency versions are pinned deliberately; bump them as coordinated
        // upgrades, not one-off lint suggestions.
        disable += "GradleDependency"
    }
}

// Fail fast only for tasks that actually produce shippable release artifacts.
// lintRelease / testReleaseUnitTest etc. must stay runnable without a keystore,
// and CI may build unsigned release artifacts to validate the R8 configuration.
val signedReleaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.substringAfterLast(":").let { name ->
        name.equals("assembleRelease", ignoreCase = true) ||
            name.equals("bundleRelease", ignoreCase = true)
    }
}
val allowUnsignedRelease =
    providers.environmentVariable("QBR_ALLOW_UNSIGNED_RELEASE").orNull == "1"
if (signedReleaseTaskRequested && !hasReleaseSigningConfig && !allowUnsignedRelease) {
    throw GradleException(
        "Release signing config is missing. " +
            "Create keystore.properties from keystore.properties.example " +
            "or pass RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD. " +
            "(CI can set QBR_ALLOW_UNSIGNED_RELEASE=1 to build unsigned release artifacts.)"
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
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("com.google.errorprone:error_prone_annotations:2.28.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

