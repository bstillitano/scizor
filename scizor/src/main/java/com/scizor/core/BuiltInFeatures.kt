package com.scizor.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import com.scizor.feature.console.ConsoleScreen
import com.scizor.feature.custom.EnvironmentVariablesScreen
import com.scizor.feature.featureflags.FeatureFlagsScreen
import com.scizor.feature.network.NetworkScreen
import com.scizor.feature.preferences.PreferencesScreen
import com.scizor.feature.servers.ServersScreen

/**
 * Registers Scizor's built-in feature screens into the [FeatureRegistry].
 * Called once from [com.scizor.Scizor.start]. Sections mirror Scyther's grouping.
 * Device & Application facts are rendered inline by the menu, not as an entry.
 */
internal fun registerBuiltInFeatures() {
    // Networking
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "network",
            title = "Network Logger",
            subtitle = "Inspect HTTP traffic, export cURL",
            icon = Icons.Filled.CompareArrows,
            section = "Networking",
            screen = { NetworkScreen(it) },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "servers",
            title = "Server Configuration",
            subtitle = "Switch backend environment",
            icon = Icons.Filled.Dns,
            section = "Networking",
            screen = { ServersScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "environment_variables",
            title = "Environment Variables",
            subtitle = "Host-provided key/value pairs",
            icon = Icons.Filled.Code,
            section = "Networking",
            screen = { EnvironmentVariablesScreen() },
        ),
    )

    // Data
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "feature_flags",
            title = "Feature Flags",
            subtitle = "Override flags at runtime",
            icon = Icons.Filled.Flag,
            section = "Data",
            screen = { FeatureFlagsScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "preferences",
            title = "Preferences Browser",
            subtitle = "View and edit SharedPreferences",
            icon = Icons.Filled.Tune,
            section = "Data",
            screen = { PreferencesScreen() },
        ),
    )

    // System Tools
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "console",
            title = "Console Logger",
            subtitle = "Live Logcat output",
            icon = Icons.Filled.Terminal,
            section = "System Tools",
            screen = { ConsoleScreen() },
        ),
    )
}
