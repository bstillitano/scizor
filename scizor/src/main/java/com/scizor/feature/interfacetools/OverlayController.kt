package com.scizor.feature.interfacetools

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.scizor.core.ScizorActivity

/**
 * Attaches a [ScizorOverlayView] on top of each of the host app's Activities via
 * lifecycle callbacks, so the interface tools can draw over any screen. The
 * Scizor menu itself is skipped.
 *
 * The activity's [Window.Callback] is wrapped so the overlay observes every touch
 * (all pointers, moves and lifts) without consuming them — this is what lets the
 * touch visualiser follow dragging fingers instead of only marking taps.
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
            val window = activity.window
            if (window.callback !is TouchSpyCallback) {
                window.callback = TouchSpyCallback(window.callback, overlay)
            }
        }
    }

    /**
     * Delegates every [Window.Callback] method to [delegate] but forwards touch
     * events to [overlay] first. Never consumes or alters the event.
     */
    private class TouchSpyCallback(
        private val delegate: Window.Callback,
        private val overlay: ScizorOverlayView,
    ) : Window.Callback by delegate {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            runCatching { overlay.feedTouch(event) }
            return delegate.dispatchTouchEvent(event)
        }
    }
}
