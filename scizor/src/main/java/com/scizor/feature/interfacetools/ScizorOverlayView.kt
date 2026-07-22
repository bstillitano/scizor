package com.scizor.feature.interfacetools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * A pass-through overlay added on top of each Activity's content. Draws the
 * active interface tools (grid, view bounds, touch ripples, FPS) without
 * consuming touch events — [onTouchEvent] always returns false.
 */
internal class ScizorOverlayView(context: Context) : View(context) {

    private data class Touch(val x: Float, val y: Float, val at: Long)

    private val touchPoints = ArrayDeque<Touch>()
    private var lastFrameNanos = 0L
    private var currentFps = 0

    private val density = resources.displayMetrics.density

    private val gridPaint = Paint().apply { color = 0x22000000; strokeWidth = 1f }
    private val boundsPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = 0x8800B0FF.toInt()
        strokeWidth = 1.5f * density
    }
    private val touchPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val fpsBg = Paint().apply { color = 0xCC000000.toInt() }
    private val fpsText = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 14f * density
        isAntiAlias = true
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameNanos != 0L) {
                val delta = frameTimeNanos - lastFrameNanos
                if (delta > 0) currentFps = (1_000_000_000.0 / delta).toInt()
            }
            lastFrameNanos = frameTimeNanos
            if (InterfaceToolkit.anyOverlayActive) invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (InterfaceToolkit.touches.value && event.actionMasked == MotionEvent.ACTION_DOWN) {
            touchPoints.addLast(Touch(event.x, event.y, System.currentTimeMillis()))
            invalidate()
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        if (InterfaceToolkit.grid.value) drawGrid(canvas)
        if (InterfaceToolkit.viewBounds.value) drawBounds(canvas)
        if (InterfaceToolkit.touches.value) drawTouches(canvas)
        if (InterfaceToolkit.fps.value) drawFps(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val step = 8f * density
        var x = step
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        var y = step
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }

    private fun drawBounds(canvas: Canvas) {
        val root = parent as? ViewGroup ?: return
        val overlayLoc = IntArray(2).also { getLocationInWindow(it) }
        fun visit(view: View) {
            if (view === this) return
            if (view.width > 0 && view.height > 0 && view.visibility == VISIBLE) {
                val loc = IntArray(2).also { view.getLocationInWindow(it) }
                val left = (loc[0] - overlayLoc[0]).toFloat()
                val top = (loc[1] - overlayLoc[1]).toFloat()
                canvas.drawRect(left, top, left + view.width, top + view.height, boundsPaint)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) visit(view.getChildAt(i))
            }
        }
        for (i in 0 until root.childCount) visit(root.getChildAt(i))
    }

    private fun drawTouches(canvas: Canvas) {
        val now = System.currentTimeMillis()
        while (touchPoints.isNotEmpty() && now - touchPoints.first().at > FADE_MS) {
            touchPoints.removeFirst()
        }
        touchPoints.forEach { touch ->
            val progress = (now - touch.at) / FADE_MS.toFloat()
            val radius = (24f + progress * 48f) * density
            val alpha = (170 * (1f - progress)).toInt().coerceIn(0, 255)
            touchPaint.color = (alpha shl 24) or 0x00FF4081
            canvas.drawCircle(touch.x, touch.y, radius, touchPaint)
        }
    }

    private fun drawFps(canvas: Canvas) {
        val label = "FPS: $currentFps"
        val pad = 8f * density
        val boxLeft = 16f * density
        val boxTop = 40f * density
        val textWidth = fpsText.measureText(label)
        val boxHeight = fpsText.textSize + pad * 2
        canvas.drawRect(boxLeft, boxTop, boxLeft + textWidth + pad * 2, boxTop + boxHeight, fpsBg)
        canvas.drawText(label, boxLeft + pad, boxTop + pad + fpsText.textSize, fpsText)
    }

    private companion object {
        const val FADE_MS = 800L
    }
}
