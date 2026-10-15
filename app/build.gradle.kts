plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// GitHub Actions can supply the EXACT signing certificate already used on the phone.
// No private keys or passwords are saved in the source repository.
val hmyzakSigningStore = System.getenv("HMYZAK_SIGNING_STORE_FILE")

android {
    namespace = "cz.rezim.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "cz.rezim.app"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("HMYZAK_VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = System.getenv("HMYZAK_VERSION_NAME") ?: "0.2.0"
    }
    // Release builds in CI use the SAME key as the installed original debug APK.
    // Do not change keys unless you intentionally migrate stored app data.
    signingConfigs {
        create("hmyzakCi") {
            if (!hmyzakSigningStore.isNullOrBlank()) {
                storeFile = file(hmyzakSigningStore)
                storePassword = System.getenv("HMYZAK_SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("HMYZAK_SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("HMYZAK_SIGNING_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (!hmyzakSigningStore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("hmyzakCi")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
