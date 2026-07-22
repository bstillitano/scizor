package com.scizor.feature.deeplink

/**
 * A host-registered deep link shown as a one-tap preset in the Deep Link Tester.
 * Register via [com.scizor.Scizor.deepLinkPresets].
 */
data class DeepLinkPreset(
    val name: String,
    val url: String,
)
