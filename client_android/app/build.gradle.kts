import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.spundev.webrtcshare"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.spundev.webrtcshare"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Use debug signingConfigs to create release builds during development
            signingConfig = signingConfigs.named("debug").get()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    // Import Compose BoM
    implementation(platform(libs.androidx.compose.bom))
    // Import Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Activity
    implementation(libs.androidx.activity.compose)
    // Compose
    implementation(libs.androidx.compose.material3)
    // Compose tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Firebase
    implementation(libs.firebase.database)
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // After updating to Kotlin 2.3.0, we got this error when running the app:
    //     [Hilt] Provided Metadata instance has version 2.3.0, while maximum supported version is 2.2.0.
    // Reports in the Dagger repository were closed saying that this was not something they should fix.
    // To fix it ourselves, we need to add the dependency with the correct version manually.
    // NOTE: We are not moving this to libs.versions.toml to make it clear that this is a temp fix and
    //  that we should try to remove this line in the future to check if they changed their mind.
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")
    // Hilt + AndroidX
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    // Kotlinx serialization
    implementation(libs.kotlinx.serialization.json)
    // Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // Timber
    implementation(libs.timber)
    // zxing
    implementation(libs.zxing)
    // WebRTC
    implementation(libs.getstream.webrtc.android)

    // TESTS
    // For instrumentation tests
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    // For local unit tests
    testImplementation(libs.androidx.test.ext)
}