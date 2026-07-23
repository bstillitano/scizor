plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.scizor.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.scizor.sample"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Use the debug signing config so assembleRelease works locally.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    debugImplementation(project(":scizor"))
    releaseImplementation(project(":scizor-no-op"))

    // Enables the Deep Link Tester's QR scanner in debug builds (optional in Scizor).
    debugImplementation(libs.play.services.code.scanner)

    // Enables Scizor's fused-provider location mocking (optional in Scizor).
    debugImplementation(libs.play.services.location)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.okhttp)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // OpenStreetMap view for the Location tab (no API key required).
    implementation(libs.osmdroid.android)
}
