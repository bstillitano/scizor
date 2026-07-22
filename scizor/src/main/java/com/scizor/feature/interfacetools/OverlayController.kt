package com.scizor.feature.interfacetools

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager

/**
 * Hosts the interface-tool overlays in a single full-screen system window
 * ([WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]) so grid, FPS, touches and
 * frames draw over everything — the host app, the Scizor menu and the system bars.
 *
 * The window is `FLAG_NOT_TOUCHABLE`, so it never intercepts input; the touch
 * visualiser is instead fed from each foreground Activity's [Window.Callback]. The
 * overlay is added only while a tool is active and the "Display over other apps"
 * permission is granted.
 */
internal object OverlayController {

    private var appContext: Context? = null
    private var overlay: ScizorOverlayView? = null
    private var foreground: Activity? = null

    fun install(application: Application) {
        appContext = application.applicationContext
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    foreground = activity
                    wrapCallback(activity)
                    refresh()
                }

                override fun onActivityPaused(activity: Activity) {
                    if (foreground === activity) foreground = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
        InterfaceToolkit.onOverlayChanged = { refresh() }
    }

    /** Whether the "Display over other apps" permission is granted. */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Adds, removes, or retargets the overlay window based on tool state + permission. */
    fun refresh() {
        val ctx = appContext ?: return
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val shouldShow = InterfaceToolkit.anyOverlayActive && canDrawOverlays(ctx)
        if (shouldShow && overlay == null) {
            runCatching {
                val view = ScizorOverlayView(ctx)
                wm.addView(view, overlayParams())
                overlay = view
            }
        } else if (!shouldShow && overlay != null) {
            runCatching { wm.removeView(overlay) }
            overlay = null
        }
        overlay?.contentRoot = foreground?.window?.decorView
    }

    @Suppress("DEPRECATION")
    private fun overlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
    }

    private fun wrapCallback(activity: Activity) {
        val window = activity.window
        if (window.callback !is TouchSpyCallback) {
            window.callback = TouchSpyCallback(window.callback)
        }
    }

    /** Forwards touch events to the current overlay without consuming them. */
    private class TouchSpyCallback(private val delegate: Window.Callback) : Window.Callback by delegate {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            runCatching { overlay?.feedTouch(event) }
            return delegate.dispatchTouchEvent(event)
        }
    }
}
