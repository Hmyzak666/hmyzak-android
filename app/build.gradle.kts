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

// Explicit opt-in for Play distribution. The public verification key is not a secret.
val liftantTestPro = providers.gradleProperty("liftantTestPro").orNull == "true"
val liftantPlayBuild = providers.gradleProperty("liftantPlayBuild").orNull == "true"
val liftantPlayKey = rootProject.file("config/play-billing-public-key.txt").takeIf {it.exists()}?.readText()?.trim().orEmpty()
require(liftantPlayKey.matches(Regex("[A-Za-z0-9+/=]*"))) { "Invalid Play public key encoding" }
val liftantUploadStore = System.getenv("LIFTANT_UPLOAD_STORE_FILE")
if(liftantPlayBuild) {
    require(!liftantTestPro) { "Test Pro must never be enabled for Play" }
    require(liftantPlayKey.isNotBlank()) { "Configure the real Play public key before building for Play" }
    require(hmyzakSigningStore.isNullOrBlank()) { "The original compatibility key must not be used for Play" }
    require(!liftantUploadStore.isNullOrBlank()) { "Configure a separate Play upload keystore" }
}

android {
    namespace = "cz.rezim.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "cz.rezim.app"
        buildConfigField("boolean","PLAY_DISTRIBUTION",liftantPlayBuild.toString())
        buildConfigField("boolean","TEST_PRO_UNLOCK",liftantTestPro.toString())
        buildConfigField("String","PLAY_BILLING_PUBLIC_KEY","\"$liftantPlayKey\"")
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("HMYZAK_VERSION_CODE")?.toIntOrNull()
            ?: hmyzakVersion.getProperty("versionBaseCode").toInt()
        versionName = System.getenv("HMYZAK_VERSION_NAME")
            ?: "${hmyzakVersion.getProperty("versionName")}.0"
    }
    // Release builds in CI use the SAME key as the installed original debug APK.
    // Do not change keys unless you intentionally migrate stored app data.
    signingConfigs {
        create("liftantPlay") {
            if(liftantPlayBuild && !liftantUploadStore.isNullOrBlank()) {
                storeFile=file(liftantUploadStore)
                storePassword=System.getenv("LIFTANT_UPLOAD_STORE_PASSWORD")
                keyAlias=System.getenv("LIFTANT_UPLOAD_KEY_ALIAS")
                keyPassword=System.getenv("LIFTANT_UPLOAD_KEY_PASSWORD")
            }
        }
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
            if(liftantPlayBuild) signingConfig=signingConfigs.getByName("liftantPlay")
            else if (!hmyzakSigningStore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("hmyzakCi")
            }
        }
    }
    // Keep all six languages available for the in-app picker, including offline.
    bundle { language { enableSplit = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
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
    implementation("com.android.billingclient:billing:9.1.0")
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
    testImplementation("androidx.compose.ui:ui-test-junit4")
}

// Release assembly always includes the regression and database compatibility tests.
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn("testReleaseUnitTest")
}
