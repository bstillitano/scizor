package com.scizor.feature.interfacetools

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.scizor.core.ScizorActivity

/**
 * Attaches a [ScizorOverlayView] on top of each of the host app's Activities via
 * lifecycle callbacks, so the interface tools can draw over any screen. The
 * Scizor menu itself is skipped.
 */
internal object OverlayController {

    private const val TAG = "scizor_overlay"

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) = attach(activity)
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    private fun attach(activity: Activity) {
        if (activity is ScizorActivity) return
        val decor = activity.window?.decorView as? ViewGroup ?: return
        if (decor.findViewWithTag<View>(TAG) != null) return
        runCatching {
            val overlay = ScizorOverlayView(activity).apply { tag = TAG }
            decor.addView(
                overlay,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }
}
