package com.scizor

import android.app.Application
import com.scizor.feature.console.ConsoleLogger
import com.scizor.feature.custom.DeveloperOption
import com.scizor.feature.featureflags.FeatureFlags
import com.scizor.feature.network.NetworkLogger
import com.scizor.feature.preferences.PreferencesBrowser
import com.scizor.feature.servers.ServerConfiguration

/**
 * No-op build of the Scizor facade. Exposes the same public API as the real
 * `scizor` artifact but does nothing, so it can be depended on in release builds
 * via `releaseImplementation`.
 */
object Scizor {

    var invocationGesture: ScizorGesture = ScizorGesture.SHAKE

    var developerOptions: List<DeveloperOption> = emptyList()

    var environmentVariables: Map<String, String> = emptyMap()

    var fcmToken: String? = null

    var interfacePreviews: List<com.scizor.feature.interfacepreviews.InterfacePreview> = emptyList()

    var deepLinkPresets: List<com.scizor.feature.deeplink.DeepLinkPreset> = emptyList()

    val network: NetworkLogger get() = NetworkLogger

    val featureFlags: FeatureFlags get() = FeatureFlags

    val servers: ServerConfiguration get() = ServerConfiguration

    val console: ConsoleLogger get() = ConsoleLogger

    val preferences: PreferencesBrowser get() = PreferencesBrowser

    @Suppress("UNUSED_PARAMETER")
    fun start(application: Application) = Unit

    fun show() = Unit
}
