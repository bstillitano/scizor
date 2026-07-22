package com.scizor.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import com.scizor.feature.deviceinfo.DeviceInfoScreen

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
}
