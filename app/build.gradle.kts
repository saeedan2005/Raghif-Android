plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val releaseKeystoreFile = System.getenv("RAGHIF_KEYSTORE_FILE")
val releaseKeystorePassword = System.getenv("RAGHIF_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("RAGHIF_KEY_ALIAS")
val releaseKeyPassword = System.getenv("RAGHIF_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.raghif.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.raghif.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "رغيف"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Match the original Raghif package and launcher name used by prior builds.
            // No preview suffix: applicationId remains com.raghif.app.
            manifestPlaceholders["appLabel"] = "رغيف"
        }
        getByName("release") {
            // The official package has no preview suffix and uses the production key.
            // CI only builds this variant when all signing credentials are configured.
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    testImplementation("junit:junit:4.13.2")
}
