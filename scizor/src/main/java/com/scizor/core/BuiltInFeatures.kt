package com.scizor.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import com.scizor.feature.console.ConsoleScreen
import com.scizor.feature.deviceinfo.DeviceInfoScreen
import com.scizor.feature.featureflags.FeatureFlagsScreen
import com.scizor.feature.network.NetworkScreen
import com.scizor.feature.preferences.PreferencesScreen
import com.scizor.feature.servers.ServersScreen

/**
 * Registers Scizor's built-in feature screens into the [FeatureRegistry].
 * Called once from [com.scizor.Scizor.start]. Each new feature adds one entry.
 */
internal fun registerBuiltInFeatures() {
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "device_info",
            title = "Device & App Info",
            subtitle = "Model, OS, version, package",
            icon = Icons.Filled.PhoneAndroid,
            section = "Application",
            screen = { DeviceInfoScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "feature_flags",
            title = "Feature Flags",
            subtitle = "Override flags at runtime",
            icon = Icons.Filled.Flag,
            section = "Configuration",
            screen = { FeatureFlagsScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "servers",
            title = "Server Configuration",
            subtitle = "Switch backend environment",
            icon = Icons.Filled.Dns,
            section = "Configuration",
            screen = { ServersScreen() },
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
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "network",
            title = "Network Logger",
            subtitle = "Inspect HTTP traffic, export cURL",
            icon = Icons.Filled.CompareArrows,
            section = "Networking",
            screen = { NetworkScreen() },
        ),
    )
    FeatureRegistry.register(
        ScizorMenuEntry(
            id = "console",
            title = "Console Logger",
            subtitle = "Live Logcat output",
            icon = Icons.Filled.Terminal,
            section = "Diagnostics",
            screen = { ConsoleScreen() },
        ),
    )
}
