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
 * active interface tools (grid, view frames, view sizes, touch ripples, FPS)
 * without consuming touch events — [onTouchEvent] always returns false.
 */
internal class ScizorOverlayView(context: Context) : View(context) {

    private data class Touch(val x: Float, val y: Float, val at: Long)

    private val touchPoints = ArrayDeque<Touch>()
    private var lastFrameNanos = 0L
    private var currentFps = 0
    private val fpsHistory = ArrayDeque<Int>()

    private val density = resources.displayMetrics.density

    private val gridPaint = Paint().apply { strokeWidth = 1f }
    private val boundsPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = 0x8800B0FF.toInt()
        strokeWidth = 1.5f * density
    }
    private val sizeLabelBg = Paint().apply { color = 0xCC0091EA.toInt() }
    private val sizeLabelText = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 9f * density
        isAntiAlias = true
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
                if (delta > 0) {
                    currentFps = (1_000_000_000.0 / delta).toInt().coerceAtMost(240)
                    fpsHistory.addLast(currentFps)
                    if (fpsHistory.size > 60) fpsHistory.removeFirst()
                }
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
            InterfaceToolkit.logTouch(event.x, event.y)
            invalidate()
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        if (InterfaceToolkit.grid.value) drawGrid(canvas)
        if (InterfaceToolkit.frames.value || InterfaceToolkit.sizes.value) drawBounds(canvas)
        if (InterfaceToolkit.touches.value) drawTouches(canvas)
        if (InterfaceToolkit.fps.value) drawFps(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val alpha = (InterfaceToolkit.gridOpacity.value * 255 / 100).coerceIn(0, 255)
        gridPaint.color = (alpha shl 24)
        val step = InterfaceToolkit.gridSizeDp.value * density
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
        val drawFrames = InterfaceToolkit.frames.value
        val drawSizes = InterfaceToolkit.sizes.value
        val overlayLoc = IntArray(2).also { getLocationInWindow(it) }
        fun visit(view: View) {
            if (view === this) return
            if (view.width > 0 && view.height > 0 && view.visibility == VISIBLE) {
                val loc = IntArray(2).also { view.getLocationInWindow(it) }
                val left = (loc[0] - overlayLoc[0]).toFloat()
                val top = (loc[1] - overlayLoc[1]).toFloat()
                if (drawFrames) {
                    canvas.drawRect(left, top, left + view.width, top + view.height, boundsPaint)
                }
                if (drawSizes) drawSizeLabel(canvas, view, left, top)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) visit(view.getChildAt(i))
            }
        }
        for (i in 0 until root.childCount) visit(root.getChildAt(i))
    }

    private fun drawSizeLabel(canvas: Canvas, view: View, left: Float, top: Float) {
        val w = (view.width / density).toInt()
        val h = (view.height / density).toInt()
        val label = "${w}×${h}"
        val pad = 2f * density
        val tw = sizeLabelText.measureText(label)
        val th = sizeLabelText.textSize
        canvas.drawRect(left, top, left + tw + pad * 2, top + th + pad * 2, sizeLabelBg)
        canvas.drawText(label, left + pad, top + th + pad, sizeLabelText)
    }

    private fun drawTouches(canvas: Canvas) {
        val fadeMs = InterfaceToolkit.touchFadeMs.value.toLong()
        val maxRadius = InterfaceToolkit.touchRadiusDp.value * density
        val now = System.currentTimeMillis()
        while (touchPoints.isNotEmpty() && now - touchPoints.first().at > fadeMs) {
            touchPoints.removeFirst()
        }
        touchPoints.forEach { touch ->
            val progress = (now - touch.at) / fadeMs.toFloat()
            val radius = maxRadius * (0.5f + progress)
            val alpha = (170 * (1f - progress)).toInt().coerceIn(0, 255)
            touchPaint.color = (alpha shl 24) or 0x00FF4081
            canvas.drawCircle(touch.x, touch.y, radius, touchPaint)
        }
    }

    private fun drawFps(canvas: Canvas) {
        val averaged = InterfaceToolkit.fpsAveraged.value
        val value = if (averaged && fpsHistory.isNotEmpty()) fpsHistory.average().toInt() else currentFps
        val label = "FPS: $value"
        fpsText.color = when {
            value <= InterfaceToolkit.fpsCritical.value -> 0xFFFF5252.toInt()
            value <= InterfaceToolkit.fpsWarn.value -> 0xFFFFC107.toInt()
            else -> 0xFF69F0AE.toInt()
        }
        val pad = 8f * density
        val textWidth = fpsText.measureText(label)
        val boxWidth = textWidth + pad * 2
        val boxHeight = fpsText.textSize + pad * 2
        val margin = 16f * density
        val (boxLeft, boxTop) = when (InterfaceToolkit.fpsCorner.value) {
            InterfaceToolkit.Corner.TOP_LEFT -> margin to margin * 2.5f
            InterfaceToolkit.Corner.TOP_RIGHT -> (width - boxWidth - margin) to margin * 2.5f
            InterfaceToolkit.Corner.BOTTOM_LEFT -> margin to (height - boxHeight - margin)
            InterfaceToolkit.Corner.BOTTOM_RIGHT -> (width - boxWidth - margin) to (height - boxHeight - margin)
        }
        canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, fpsBg)
        canvas.drawText(label, boxLeft + pad, boxTop + pad + fpsText.textSize, fpsText)
    }
}
