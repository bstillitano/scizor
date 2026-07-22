plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.scizor"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
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
    // Only what the mirrored public signatures require.
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    implementation(libs.compose.ui)
}
