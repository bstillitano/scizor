package com.scizor.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.scizor.Scizor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Draws a small draggable button over each foreground Activity that opens the
 * Scizor menu on tap. Used when [com.scizor.ScizorGesture.FLOATING_BUTTON] is
 * selected. The button lives inside the app's own window (added via
 * [Activity.addContentView]), so — unlike the interface-tool overlays — it needs
 * no "Display over other apps" permission.
 */
internal object FloatingButtonController {

    private var enabled = false
    private var foreground: Activity? = null
    private var button: View? = null

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    if (activity is ScizorActivity) return // never over the menu itself
                    foreground = activity
                    if (enabled) attach(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (foreground === activity) {
                        detach()
                        foreground = null
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    /** Enables or disables the floating button, attaching to the current Activity immediately. */
    fun setEnabled(on: Boolean) {
        enabled = on
        if (on) foreground?.let { attach(it) } else detach()
    }

    private fun attach(activity: Activity) {
        if (button?.context === activity) return
        detach()
        val density = activity.resources.displayMetrics.density
        val margin = (16 * density).roundToInt()
        val params = FrameLayout.LayoutParams(
            (52 * density).roundToInt(),
            (52 * density).roundToInt(),
            Gravity.BOTTOM or Gravity.END,
        ).apply {
            rightMargin = margin
            bottomMargin = margin * 4 // clear of the nav bar / gesture pill
        }
        val view = createButton(activity)
        runCatching { activity.addContentView(view, params) }
        button = view
    }

    private fun detach() {
        (button?.parent as? ViewGroup)?.removeView(button)
        button = null
    }

    @SuppressLint("SetTextI18n")
    private fun createButton(activity: Activity): View {
        val density = activity.resources.displayMetrics.density
        return TextView(activity).apply {
            text = "🐞"
            textSize = 22f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE6202124.toInt())
                setStroke((1 * density).roundToInt(), 0x66FFFFFF)
            }
            elevation = 8 * density
            isClickable = true
            setOnClickListener { Scizor.show() }
            setOnTouchListener(DragTapListener())
        }
    }

    /** Drags the button around, and treats a press that barely moves as a tap. */
    private class DragTapListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startTransX = 0f
        private var startTransY = 0f
        private var dragged = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val slop = ViewConfiguration.get(v.context).scaledTouchSlop
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startTransX = v.translationX
                    startTransY = v.translationY
                    dragged = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragged = true
                    val parent = v.parent as? View
                    val maxLeft = -((parent?.width ?: 0) - v.width).toFloat()
                    val maxUp = -((parent?.height ?: 0) - v.height).toFloat()
                    v.translationX = (startTransX + dx).coerceIn(maxLeft.coerceAtMost(0f), 0f)
                    v.translationY = (startTransY + dy).coerceIn(maxUp.coerceAtMost(0f), 0f)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) v.performClick()
                    return true
                }
            }
            return false
        }
    }
}
