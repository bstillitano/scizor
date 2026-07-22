package com.scizor.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import com.scizor.feature.deviceinfo.DeviceInfoScreen
import com.scizor.feature.featureflags.FeatureFlagsScreen
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
}
