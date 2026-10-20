import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

// Release signing comes ONLY from CI secrets. No release key is ever committed.
val releaseKeystoreB64: String? = System.getenv("VG_RELEASE_KEYSTORE_B64")
val releaseKeystoreFile = layout.buildDirectory.file("secrets/release.jks").get().asFile

android {
    namespace = "com.victorious.gps"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.victorious.gps"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/victorgps-debug.jks")
            storePassword = "victorgps-debug"
            keyAlias = "victorgps-debug"
            keyPassword = "victorgps-debug"
        }
        if (releaseKeystoreB64 != null) {
            create("release") {
                releaseKeystoreFile.parentFile.mkdirs()
                releaseKeystoreFile.writeBytes(Base64.getDecoder().decode(releaseKeystoreB64))
                storeFile = releaseKeystoreFile
                storePassword = System.getenv("VG_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("VG_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("VG_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("debug") }
        release {
            isMinifyEnabled = false
            if (releaseKeystoreB64 != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}") }
}

kapt { correctErrorTypes = true }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Map without an API key.
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // FusedLocationProviderClient.setMockMode where Play services exist.
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
