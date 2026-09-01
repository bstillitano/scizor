package com.scizor

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * Wire the artifact with `debugImplementation` so it is not in release builds
 * at all. If you do ship it in a release build, [start] refuses to run unless
 * you pass `allowProductionBuilds = true`.
 */
object Scizor {

    private const val TAG = "Scizor"

    private var application: Application? = null
    private var shakeDetector: ShakeDetector? = null

    /**
     * The live menu activity, if one is open.
     *
     * Weak so a missed [ScizorActivity.onDestroy] can never leak an activity.
     */
    @Volatile
    internal var activityRef: java.lang.ref.WeakReference<ScizorActivity>? = null

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
    var developerOptions: List<DeveloperOption> by mutableStateOf(emptyList())

    /** Read-only key/value pairs surfaced in the menu's environment screen. */
    var environmentVariables: Map<String, String> by mutableStateOf(emptyMap())

    /** FCM registration token, if the host provides it — shown in Notifications. */
    var fcmToken: String? by mutableStateOf(null)

    /** Host-registered Composables shown in the menu's Interface Previews screen. */
    var interfacePreviews: List<InterfacePreview> by mutableStateOf(emptyList())

    /** One-tap deep link presets shown in the Deep Link Tester. */
    var deepLinkPresets: List<com.scizor.feature.deeplink.DeepLinkPreset> by mutableStateOf(emptyList())

    /** Host-provided custom database sources shown in the Database Browser. */
    var databaseAdapters: List<com.scizor.feature.databasebrowser.ScizorDatabaseAdapter> by mutableStateOf(emptyList())

    /**
     * Ids of built-in features to hide from the menu.
     *
     * This exists for hosts that ship the real artifact in a signed QA build,
     * where a Logcat reader or a keystore browser is a different risk
     * conversation. See the README for the full id list.
     */
    var disabledFeatures: Set<String> by mutableStateOf(emptySet())

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
     *
     * ## The production gate
     *
     * Scizor refuses to start in a build that is not debuggable. Nothing is
     * captured, installed or observed — no Logcat reader, no crash handler, no
     * overlay, no shake detector — and a warning naming the reason is written to
     * Logcat so the refusal is never silent.
     *
     * `FLAG_DEBUGGABLE` is Android's analogue of the receipt check Scyther makes
     * on iOS. It is set for debug builds and for internal-distribution builds
     * deliberately signed as debuggable, and clear for anything shipped through
     * the Play Store.
     *
     * @param allowProductionBuilds start even when the build is not debuggable.
     *   For a host that ships the real artifact on purpose — a signed QA build,
     *   or a menu unlocked behind a hidden gesture. Pair it with
     *   [disabledFeatures] to keep the riskier tools out of a build that leaves
     *   the building.
     */
    fun start(application: Application, allowProductionBuilds: Boolean = false) {
        // Checked ahead of the already-started guard, mirroring Scyther's
        // `guard !AppEnvironment.isAppStore || allowProductionBuilds` as the first
        // line of its own start(), so that every refused call says why rather than
        // only the first one. A refused call does nothing either way.
        val debuggable =
            (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable && !allowProductionBuilds) {
            android.util.Log.w(
                TAG,
                "start() refused — this build is not debuggable, so Scizor did not start. " +
                    "Scizor is meant to be wired with debugImplementation; if you mean to " +
                    "ship it in a release build, call " +
                    "Scizor.start(application, allowProductionBuilds = true).",
            )
            return
        }
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
        com.scizor.core.FloatingButtonController.install(application)
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
        val context = application
        if (context == null) {
            android.util.Log.w(TAG, "show() ignored — Scizor.start(application) has not been called")
            return
        }
        runCatching {
            val intent = Intent(context, ScizorActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure {
            android.util.Log.w(
                TAG,
                "show() failed to start ScizorActivity — check that the scizor artifact " +
                    "is on this variant's classpath so its manifest entry is merged in",
                it,
            )
        }
    }

    /** Closes the debug menu if it is open. No-op otherwise. */
    fun dismiss() {
        activityRef?.get()?.finish()
    }

    private fun applyInvocationGesture(context: Context) {
        shakeDetector?.stop()
        shakeDetector = null
        if (invocationGesture == ScizorGesture.SHAKE) {
            shakeDetector = ShakeDetector(onShake = { show() }).also { it.start(context) }
        }
        com.scizor.core.FloatingButtonController.setEnabled(invocationGesture == ScizorGesture.FLOATING_BUTTON)
    }
}
