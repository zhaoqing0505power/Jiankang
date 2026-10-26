plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// Signing material comes from local environment / GitHub Secrets and never from source.
val stableSigningPath = System.getenv("JIANKANG_SIGNING_STORE_FILE").orEmpty()
val stableSigningStorePassword = System.getenv("JIANKANG_SIGNING_STORE_PASSWORD").orEmpty()
val stableSigningKeyPassword = System.getenv("JIANKANG_SIGNING_KEY_PASSWORD").orEmpty()
val stableSigningAlias = System.getenv("JIANKANG_SIGNING_KEY_ALIAS").orEmpty()
val stableSigningAvailable = listOf(
    stableSigningPath, stableSigningStorePassword, stableSigningKeyPassword, stableSigningAlias
).all { it.isNotBlank() }
if (stableSigningPath.isNotBlank()) {
    require(stableSigningAvailable) { "Stable signer requires all four environment variables" }
    require(file(stableSigningPath).isFile) { "Missing stable signing keystore" }
}

android {
    signingConfigs {
        if (stableSigningAvailable) {
            create("stableRelease") {
                storeFile = file(stableSigningPath)
                storePassword = stableSigningStorePassword
                keyAlias = stableSigningAlias
                keyPassword = stableSigningKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    namespace = "com.jiankang.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jiankang.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (stableSigningAvailable) signingConfig = signingConfigs.getByName("stableRelease")
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
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.17.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
