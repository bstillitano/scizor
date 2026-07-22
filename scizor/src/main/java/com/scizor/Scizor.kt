package com.scizor

import android.app.Application
import android.content.Context
import android.content.Intent
import com.scizor.core.ScizorActivity
import com.scizor.core.ScizorStore
import com.scizor.core.ShakeDetector
import com.scizor.feature.custom.DeveloperOption

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

    /**
     * Wires up Scizor. Call once, early in [Application.onCreate].
     * Safe to call more than once; subsequent calls are ignored.
     */
    fun start(application: Application) {
        if (this.application != null) return
        this.application = application
        store = ScizorStore(application).also { it.preload() }
        applyInvocationGesture(application)
    }

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
