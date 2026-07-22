package com.scizor.sample

import android.app.Application
import com.scizor.Scizor
import com.scizor.ScizorGesture
import com.scizor.feature.console.ConsoleLogger
import com.scizor.feature.console.LogEntry
import com.scizor.feature.console.LogLevel
import com.scizor.feature.custom.DeveloperOption
import com.scizor.feature.deviceinfo.DeviceInfo
import com.scizor.feature.deviceinfo.InfoRow
import com.scizor.feature.featureflags.FeatureFlag
import com.scizor.feature.featureflags.FeatureFlags
import com.scizor.feature.featureflags.FlagOverride
import com.scizor.feature.network.NetworkLogger
import com.scizor.feature.network.NetworkTransaction
import com.scizor.feature.network.ScizorInterceptor
import com.scizor.feature.network.toCurl
import com.scizor.feature.preferences.PrefEntry
import com.scizor.feature.preferences.PreferencesBrowser
import com.scizor.feature.servers.ServerConfiguration
import com.scizor.feature.servers.ServerEnvironment

/**
 * Compile-time parity check between the `scizor` and `scizor-no-op` artifacts.
 *
 * This file lives in the sample's `main` source set, which compiles against
 * `scizor` for debug and `scizor-no-op` for release. If either artifact is
 * missing any referenced public symbol — or its signature drifts — one of the
 * `assembleDebug` / `assembleRelease` builds fails. It is never executed.
 */
@Suppress("unused", "UNUSED_VARIABLE", "UNUSED_EXPRESSION")
internal fun scizorApiSurface(application: Application) {
    // Facade
    Scizor.start(application)
    Scizor.show()
    Scizor.invocationGesture = ScizorGesture.SHAKE
    Scizor.invocationGesture = ScizorGesture.FLOATING_BUTTON
    Scizor.invocationGesture = ScizorGesture.NONE
    Scizor.developerOptions = listOf(
        DeveloperOption(title = "x", icon = null) {},
        DeveloperOption(title = "v", value = "1"),
        DeveloperOption(title = "s", screen = {}),
    )
    Scizor.environmentVariables = mapOf("k" to "v")
    Scizor.fcmToken = "token"
    Scizor.interfacePreviews = listOf(
        com.scizor.feature.interfacepreviews.InterfacePreview("x") {},
    )
    val network: NetworkLogger = Scizor.network
    val flags: FeatureFlags = Scizor.featureFlags
    val servers: ServerConfiguration = Scizor.servers
    val console: ConsoleLogger = Scizor.console
    val prefs: PreferencesBrowser = Scizor.preferences

    // Device info
    val rows: List<InfoRow> = DeviceInfo.collect(application)
    InfoRow(label = "l", value = "v")

    // Preferences
    prefs.files(application)
    prefs.entries(application, "file")
    prefs.putString(application, "file", "k", "v")
    prefs.putBoolean(application, "file", "k", true)
    prefs.remove(application, "file", "k")
    PrefEntry(key = "k", value = "v", type = "String")

    // Feature flags
    flags.register(FeatureFlag(key = "k", title = "t", defaultValue = false))
    flags.all()
    flags.isEnabled("k")
    flags.isOverridden("k")
    flags.remoteValue("k")
    flags.overridesEnabled = true
    flags.overrideState("k")
    flags.setOverride("k", FlagOverride.ON)
    flags.setOverride("k", FlagOverride.OFF)
    flags.setOverride("k", FlagOverride.REMOTE)
    flags.resetAllToRemote()

    // Servers
    servers.configure(listOf(ServerEnvironment(name = "n", baseUrl = "u")))
    servers.all()
    val selected: ServerEnvironment? = servers.selected
    selected?.let { servers.select(it) }
    servers.baseUrl()

    // Console
    val entries = console.entries
    console.start()
    console.stop()
    console.clear()
    LogEntry(time = "t", level = LogLevel.DEBUG, tag = "tag", message = "m")
    LogLevel.from('D')

    // Network
    val interceptor = network.interceptor()
    network.clear()
    network.find(1L)
    val txs = network.transactions
    ScizorInterceptor()
    val tx = NetworkTransaction(
        id = 1L,
        method = "GET",
        url = "https://example.com",
        requestHeaders = emptyMap(),
        requestBody = null,
        status = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 1L,
        timestamp = 0L,
        error = null,
    )
    tx.toCurl()
    tx.host
    tx.path
}
