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

        Scizor.featureFlags.register(
            FeatureFlag("new_checkout", "New checkout flow", defaultValue = false),
        )
        Scizor.featureFlags.register(
            FeatureFlag("dark_launch", "Dark launch banner", defaultValue = true),
        )
        Scizor.featureFlags.register(
            FeatureFlag("beta_search", "Beta search ranking", defaultValue = false),
        )

        Scizor.servers.configure(
            listOf(
                ServerEnvironment("Development", "https://dev.api.example.com"),
                ServerEnvironment("Staging", "https://staging.api.example.com"),
                ServerEnvironment("Production", "https://api.example.com"),
            ),
        )

        Scizor.environmentVariables = mapOf(
            "BUILD_TYPE" to "debug",
            "API_BASE_URL" to Scizor.servers.baseUrl(),
            "FLAVOR" to "sample",
            "FEATURE_SET" to "full",
        )

        Scizor.developerOptions = listOf(
            DeveloperOption(title = "Log a test message") {
                Log.i("ScizorSample", "Test log from developer option")
            },
            DeveloperOption(title = "Show a toast") {
                Toast.makeText(this, "Hello from Scizor", Toast.LENGTH_SHORT).show()
            },
        )

        seedDemoPreferences()
        Log.i("ScizorSample", "Sample app started")
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
