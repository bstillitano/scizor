package com.scizor.sample

import android.app.Application
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
        )

        Scizor.developerOptions = listOf(
            DeveloperOption(title = "Log a test message") {
                Log.i("ScizorSample", "Test log from developer option")
            },
            DeveloperOption(title = "Show a toast") {
                Toast.makeText(this, "Hello from Scizor", Toast.LENGTH_SHORT).show()
            },
        )
    }
}
