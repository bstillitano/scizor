plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

version = "0.2.1"

android {
    namespace = "com.scizor"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.okhttp)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // OpenStreetMap view for the Location Spoofer map (no API key required).
    implementation(libs.osmdroid.android)

    // Optional: the Deep Link Tester's QR scanner uses the Google Code Scanner when
    // present. compileOnly keeps it off Scizor's dependency footprint — consumers who
    // want QR scanning add `debugImplementation(libs.play.services.code.scanner)`.
    compileOnly(libs.play.services.code.scanner)

    // Optional: the Location Spoofer also mocks the fused provider (Google Play
    // Services) when present, so apps using FusedLocationProviderClient / Google Maps
    // follow the spoof. compileOnly — consumers add `debugImplementation(libs.play.services.location)`.
    compileOnly(libs.play.services.location)

    // Optional: `Scizor.network.ktorPlugin()` returns a Ktor client plugin for hosts
    // running Ktor on a non-OkHttp engine. compileOnly keeps Ktor off Scizor's
    // dependency footprint — only apps that already use Ktor ever call it.
    compileOnly(libs.ktor.client.core)

    // Optional: `Scizor.network.apolloInterceptor()` returns an Apollo Kotlin 4 HTTP
    // interceptor for hosts using Apollo on any engine. Same compileOnly deal as Ktor —
    // only apps that already ship Apollo ever load the class.
    compileOnly(libs.apollo.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.apollo.runtime)
}

// JitPack derives the group and artifact id from the repository coordinates
// (com.github.bstillitano.scizor:scizor), so only the version needs setting.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
            }
        }
    }
}
