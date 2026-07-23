package com.scizor.sample

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.scizor.Scizor
import com.scizor.feature.custom.DeveloperOption
import com.scizor.feature.featureflags.FeatureFlag
import com.scizor.feature.servers.ServerEnvironment

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Scizor.start(this)
        // A tap-to-open floating button is the easiest trigger on an emulator (shaking
        // a virtual device is awkward); on a real device SHAKE works well too.
        Scizor.invocationGesture = com.scizor.ScizorGesture.FLOATING_BUTTON

        listOf(
            "dark_mode_v2" to true,
            "new_checkout_flow" to false,
            "enhanced_search" to true,
            "push_notifications" to true,
            "biometric_login" to false,
            "analytics_v3" to true,
            "experimental_ui" to false,
            "offline_mode" to true,
        ).forEach { (key, value) ->
            Scizor.featureFlags.register(
                FeatureFlag(key, key.replace('_', ' ').replaceFirstChar { it.uppercase() }, defaultValue = value),
            )
        }

        Scizor.servers.configure(
            listOf(
                ServerEnvironment(
                    "Development", "https://dev.api.example.com",
                    variables = mapOf("cdnUrl" to "https://cdn.dev.example.com", "wsUrl" to "wss://dev.example.com"),
                ),
                ServerEnvironment(
                    "Staging", "https://staging.api.example.com",
                    variables = mapOf("cdnUrl" to "https://cdn.staging.example.com"),
                ),
                ServerEnvironment(
                    "Production", "https://api.example.com",
                    variables = mapOf("cdnUrl" to "https://cdn.example.com", "wsUrl" to "wss://example.com"),
                ),
            ),
        )

        Scizor.environmentVariables = mapOf(
            "BUILD_TYPE" to "debug",
            "API_BASE_URL" to Scizor.servers.baseUrl(),
            "FLAVOR" to "sample",
            "FEATURE_SET" to "full",
        )

        Scizor.fcmToken = "demo-fcm-token-a1b2c3d4e5f6g7h8i9j0"

        Scizor.interfacePreviews = listOf(
            com.scizor.feature.interfacepreviews.InterfacePreview(
                "Primary button",
                "The app's main call-to-action button",
            ) {
                androidx.compose.material3.Button(onClick = {}) {
                    androidx.compose.material3.Text("Click me")
                }
            },
            com.scizor.feature.interfacepreviews.InterfacePreview(
                "Loading spinner",
                "Indeterminate progress indicator",
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            },
        )

        Scizor.deepLinkPresets = listOf(
            com.scizor.feature.deeplink.DeepLinkPreset("Home", "scizorsample://home"),
            com.scizor.feature.deeplink.DeepLinkPreset("Profile", "scizorsample://user/42"),
            com.scizor.feature.deeplink.DeepLinkPreset("Settings", "scizorsample://settings"),
            com.scizor.feature.deeplink.DeepLinkPreset("Example.com", "https://example.com"),
        )

        Scizor.developerOptions = listOf(
            DeveloperOption(title = "Log a test message") {
                Log.i("ScizorSample", "Test log from developer option")
            },
            DeveloperOption(title = "Show a toast") {
                Toast.makeText(this, "Hello from Scizor", Toast.LENGTH_SHORT).show()
            },
        )

        seedDemoCookies()
        seedDemoPreferences()
        seedDemoDatabase()
        Log.i("ScizorSample", "Sample app started")
    }

    /** Registers demo cookies with Scizor so the Cookie Browser has data. */
    private fun seedDemoCookies() {
        Scizor.cookies.log(
            name = "session_id", value = "abc123def456", domain = "example.com",
            path = "/", secure = true, httpOnly = true, expires = "7 days",
        )
        Scizor.cookies.log(name = "user_prefs", value = "theme=dark&lang=en", domain = "example.com", path = "/")
        Scizor.cookies.log(name = "_ga", value = "GA1.2.1234567890.1234567890", domain = "analytics.example.com")
        Scizor.cookies.log(
            name = "auth_token", value = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", domain = "api.example.com",
            path = "/api", secure = true,
        )
    }

    /** Seeds the demo SQLite database (users, posts, products) for the Database browser. */
    private fun seedDemoDatabase() {
        SampleDatabase.seed(this)
    }

    /** Writes a spread of SharedPreferences values so the Preferences browser has data. */
    private fun seedDemoPreferences() {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
            .putString("username", "brandon")
            .putString("email", "brandon@example.com")
            .putBoolean("onboarding_complete", true)
            .putBoolean("push_enabled", false)
            .putInt("launch_count", 7)
            .putLong("last_sync_ms", 1_721_000_000_000L)
            .putFloat("cart_total", 42.5f)
            .apply()

        getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
            .putString("theme", "system")
            .putBoolean("analytics_opt_in", true)
            .apply()
    }
}
