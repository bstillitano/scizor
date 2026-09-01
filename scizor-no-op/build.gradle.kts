plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

version = "0.1.0"

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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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

    // Mirrors `Scizor.network.ktorPlugin()`'s signature only; compileOnly here too, so
    // the no-op artifact never puts Ktor on a release app's classpath.
    compileOnly(libs.ktor.client.core)
}

// JitPack derives the group and artifact id from the repository coordinates
// (com.github.bstillitano.scizor:scizor-no-op), so only the version needs setting.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
            }
        }
    }
}
