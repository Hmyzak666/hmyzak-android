import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// GitHub Actions can supply the EXACT signing certificate already used on the phone.
// No private keys or passwords are saved in the source repository.
val hmyzakSigningStore = System.getenv("HMYZAK_SIGNING_STORE_FILE")
val hmyzakVersion = Properties().apply {
    file("release-version.properties").inputStream().use { load(it) }
}

android {
    namespace = "cz.rezim.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "cz.rezim.app"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("HMYZAK_VERSION_CODE")?.toIntOrNull()
            ?: hmyzakVersion.getProperty("versionBaseCode").toInt()
        versionName = System.getenv("HMYZAK_VERSION_NAME")
            ?: "${hmyzakVersion.getProperty("versionName")}.0"
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "1g"
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
            )
            // Optional local/CI mirror for Robolectric's Android test runtime.
            System.getProperty("robolectric.dependency.repo.url")?.let { url ->
                it.systemProperty("robolectric.dependency.repo.url", url)
            }
        }
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}

// Release assembly always includes the regression and database compatibility tests.
tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn("testReleaseUnitTest")
}
