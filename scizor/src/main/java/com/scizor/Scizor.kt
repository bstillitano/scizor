package com.scizor

import android.app.Application
import android.content.Context
import android.content.Intent
import com.scizor.core.ScizorActivity
import com.scizor.core.ScizorStore
import com.scizor.core.ShakeDetector
import com.scizor.core.registerBuiltInFeatures
import com.scizor.feature.console.ConsoleLogger
import com.scizor.feature.appearance.AppearanceOverrides
import com.scizor.feature.crashlogs.CrashLogger
import com.scizor.feature.custom.DeveloperOption
import com.scizor.feature.interfacepreviews.InterfacePreview
import com.scizor.feature.interfacetools.InterfaceToolkit
import com.scizor.feature.interfacetools.OverlayController
import com.scizor.feature.featureflags.FeatureFlags
import com.scizor.feature.network.NetworkLogger
import com.scizor.feature.preferences.PreferencesBrowser
import com.scizor.feature.servers.ServerConfiguration

/**
 * Entry point to the Scizor debugging toolkit — the Android counterpart to the
 * iOS `Scyther` facade.
 *
 * Initialise once from your [Application]:
 * ```
 * Scizor.start(this)
 * ```
 * then shake the device (default) or call [show] to open the debug menu.
 *
 * In release builds, depend on the `scizor-no-op` artifact instead, which
 * provides this same API as no-ops.
 */
object Scizor {

    private var application: Application? = null
    private var shakeDetector: ShakeDetector? = null

    internal lateinit var store: ScizorStore
        private set

    /** The store if [start] has run, else null — for features that may be touched pre-start. */
    internal fun storeOrNull(): ScizorStore? = if (::store.isInitialized) store else null

    /** Gesture that opens the menu. Defaults to [ScizorGesture.SHAKE]. */
    var invocationGesture: ScizorGesture = ScizorGesture.SHAKE
        set(value) {
            field = value
            application?.let { applyInvocationGesture(it) }
        }

    /** Custom entries shown in the menu's "Developer" section. */
    var developerOptions: List<DeveloperOption> = emptyList()

    /** Read-only key/value pairs surfaced in the menu's environment screen. */
    var environmentVariables: Map<String, String> = emptyMap()

    /** FCM registration token, if the host provides it — shown in Notifications. */
    var fcmToken: String? = null

    /** Host-registered Composables shown in the menu's Interface Previews screen. */
    var interfacePreviews: List<InterfacePreview> = emptyList()

    /** One-tap deep link presets shown in the Deep Link Tester. */
    var deepLinkPresets: List<com.scizor.feature.deeplink.DeepLinkPreset> = emptyList()

    /** Host-provided custom database sources shown in the Database Browser. */
    var databaseAdapters: List<com.scizor.feature.databasebrowser.ScizorDatabaseAdapter> = emptyList()

    /** HTTP traffic logger. Add [NetworkLogger.interceptor] to your OkHttpClient. */
    val network: NetworkLogger get() = NetworkLogger

    /** Runtime-overridable feature flags. */
    val featureFlags: FeatureFlags get() = FeatureFlags

    /** Backend environment switching. */
    val servers: ServerConfiguration get() = ServerConfiguration

    /** Live Logcat capture. */
    val console: ConsoleLogger get() = ConsoleLogger

    /** SharedPreferences inspection. */
    val preferences: PreferencesBrowser get() = PreferencesBrowser

    /** Record cookies from the host so they appear in the Cookie Browser. */
    val cookies: com.scizor.feature.cookies.CookieLog get() = com.scizor.feature.cookies.CookieLog

    /**
     * Wires up Scizor. Call once, early in [Application.onCreate].
     * Safe to call more than once; subsequent calls are ignored.
     */
    fun start(application: Application) {
        if (this.application != null) return
        this.application = application
        store = ScizorStore(application).also { it.preload() }
        registerBuiltInFeatures()
        com.scizor.core.MenuPins.init()
        com.scizor.feature.deeplink.DeepLinkTester.init()
        ConsoleLogger.start()
        CrashLogger.install(application)
        InterfaceToolkit.init(store)
        OverlayController.install(application)
        AppearanceOverrides.init(application, store)
        applyInvocationGesture(application)
    }

    /**
     * Wraps [base] with the current appearance font-scale override applied. Call
     * from your `Activity.attachBaseContext(base)` to let the Appearance screen's
     * font-scale slider take effect app-wide. Returns [base] unchanged when no
     * override is set.
     */
    fun wrapAppearance(base: Context): Context =
        com.scizor.feature.appearance.AppearanceOverrides.wrap(base)

    /** Opens the debug menu. No-op if [start] has not been called. */
    fun show() {
        val context = application ?: return
        runCatching {
            val intent = Intent(context, ScizorActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun applyInvocationGesture(context: Context) {
        shakeDetector?.stop()
        shakeDetector = null
        if (invocationGesture == ScizorGesture.SHAKE) {
            shakeDetector = ShakeDetector(onShake = { show() }).also { it.start(context) }
        }
    }
}
