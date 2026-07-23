package com.scizor.feature.interfacetools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner

/**
 * A pass-through overlay added on top of each Activity's content. Draws the
 * active interface tools (grid, view frames, view sizes, touch visualiser, FPS).
 *
 * The touch visualiser tracks every active pointer's live position — fed from the
 * window's `dispatchTouchEvent` via [feedTouch] so it follows dragging fingers
 * rather than only marking taps. The view itself never consumes touches.
 */
internal class ScizorOverlayView(context: Context) : View(context) {

    private data class Touch(val x: Float, val y: Float, val at: Long)

    /** Live position of each active pointer, keyed by pointer id — follows the finger. */
    private val activePointers = HashMap<Int, PointF>()

    /** Fading marks left behind when a pointer lifts. */
    private val trails = ArrayDeque<Touch>()

    /** When each active pointer went down, for the touch-duration readout. */
    private val pointerDownAt = HashMap<Int, Long>()

    /** Live contact radius (px) per active pointer from MotionEvent.getTouchMajor. */
    private val pointerRadius = HashMap<Int, Float>()

    /**
     * The foreground activity's decor view, used to draw view frames/sizes. Since
     * this overlay lives in its own full-screen system window, everything is drawn
     * in screen coordinates.
     */
    var contentRoot: View? = null

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
    private val touchRingPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 2f * density
    }
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

    /**
     * Observes a touch event without consuming it (called from the window callback).
     * Updates each active pointer's live position so the visualiser tracks drags.
     */
    fun feedTouch(event: MotionEvent) {
        if (!InterfaceToolkit.touches.value) {
            if (activePointers.isNotEmpty()) {
                activePointers.clear()
                invalidate()
            }
            return
        }
        // Events arrive in the app window's coordinate space; this overlay is a
        // full-screen system window, so translate into screen coordinates. The raw
        // offset (screen minus local for pointer 0) is the same for every pointer.
        val offsetX = event.rawX - event.getX(0)
        val offsetY = event.rawY - event.getY(0)
        fun x(i: Int) = event.getX(i) + offsetX
        fun y(i: Int) = event.getY(i) + offsetY
        val now = System.currentTimeMillis()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                val id = event.getPointerId(i)
                activePointers[id] = PointF(x(i), y(i))
                pointerDownAt[id] = now
                pointerRadius[id] = event.getTouchMajor(i) / 2f
                InterfaceToolkit.logTouch(x(i), y(i))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    activePointers[id]?.set(x(i), y(i)) ?: run { activePointers[id] = PointF(x(i), y(i)) }
                    pointerRadius[id] = event.getTouchMajor(i) / 2f
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                activePointers.remove(id)?.let { trails.addLast(Touch(it.x, it.y, now)) }
                pointerDownAt.remove(id)
                pointerRadius.remove(id)
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointers.values.forEach { trails.addLast(Touch(it.x, it.y, now)) }
                activePointers.clear()
                pointerDownAt.clear()
                pointerRadius.clear()
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (InterfaceToolkit.grid.value) drawGrid(canvas)
        if (InterfaceToolkit.frames.value || InterfaceToolkit.sizes.value) drawBounds(canvas)
        if (InterfaceToolkit.touches.value) drawTouches(canvas)
        if (InterfaceToolkit.fps.value) drawFps(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val alpha = (InterfaceToolkit.gridOpacity.value * 255 / 100).coerceIn(0, 255)
        val rgb = when (InterfaceToolkit.gridColor.value) {
            InterfaceToolkit.GridColor.RED -> 0xFF5252
            InterfaceToolkit.GridColor.GREEN -> 0x00E676
            InterfaceToolkit.GridColor.BLUE -> 0x40C4FF
        }
        gridPaint.color = (alpha shl 24) or rgb
        val sizeDp = InterfaceToolkit.gridSizeDp.value
        val step = sizeDp * density
        if (step <= 0f) return
        // Lines are laid out symmetrically from the screen centre, matching Scyther.
        val cx = width / 2f
        val cy = height / 2f
        var x = cx
        while (x <= width) { canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint); x += step }
        x = cx - step
        while (x >= 0) { canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint); x -= step }
        var y = cy
        while (y <= height) { canvas.drawLine(0f, y, width.toFloat(), y, gridPaint); y += step }
        y = cy - step
        while (y >= 0) { canvas.drawLine(0f, y, width.toFloat(), y, gridPaint); y -= step }
        // Measurement labels: the centre gap size, on both axes.
        val label = "$sizeDp dp"
        drawGridLabel(canvas, label, cx + step / 2f, cy - 6f * density) // horizontal gap
        drawGridLabel(canvas, label, cx + 6f * density, cy + step / 2f) // vertical gap
    }

    private fun drawGridLabel(canvas: Canvas, label: String, x: Float, y: Float) {
        val pad = 2f * density
        val tw = sizeLabelText.measureText(label)
        val th = sizeLabelText.textSize
        canvas.drawRect(x, y - th, x + tw + pad * 2, y + pad * 2, fpsBg)
        canvas.drawText(label, x + pad, y, sizeLabelText)
    }

    private fun drawBounds(canvas: Canvas) {
        val root = contentRoot ?: return
        val drawFrames = InterfaceToolkit.frames.value
        val drawSizes = InterfaceToolkit.sizes.value
        val screen = IntArray(2)
        fun visit(view: View) {
            if (view.width > 0 && view.height > 0 && view.visibility == VISIBLE) {
                // A Compose UI is one big view, so walk its semantics tree instead.
                if (view.javaClass.simpleName == "AndroidComposeView") {
                    drawComposeBounds(canvas, view, drawFrames, drawSizes)
                } else {
                    view.getLocationOnScreen(screen)
                    val left = screen[0].toFloat()
                    val top = screen[1].toFloat()
                    if (drawFrames) canvas.drawRect(left, top, left + view.width, top + view.height, boundsPaint)
                    if (drawSizes) drawSizeLabel(canvas, left, top, view.width, view.height)
                }
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) visit(view.getChildAt(i))
            }
        }
        visit(root)
    }

    /** Draws frames/sizes for a Jetpack Compose hierarchy via its semantics tree. */
    private fun drawComposeBounds(canvas: Canvas, composeView: View, drawFrames: Boolean, drawSizes: Boolean) {
        runCatching {
            val owner = composeView.javaClass.methods
                .firstOrNull { it.name == "getSemanticsOwner" }
                ?.apply { isAccessible = true }
                ?.invoke(composeView) as? SemanticsOwner ?: return
            // boundsInWindow is relative to the app window; add the window's screen origin.
            val win = IntArray(2).also { composeView.rootView.getLocationOnScreen(it) }
            fun walk(node: SemanticsNode) {
                val b = node.boundsInWindow
                if (b.width > 0f && b.height > 0f) {
                    val l = b.left + win[0]
                    val t = b.top + win[1]
                    if (drawFrames) canvas.drawRect(l, t, b.right + win[0], b.bottom + win[1], boundsPaint)
                    if (drawSizes) drawSizeLabel(canvas, l, t, b.width.toInt(), b.height.toInt())
                }
                node.children.forEach { walk(it) }
            }
            walk(owner.unmergedRootSemanticsNode)
        }
    }

    private fun drawSizeLabel(canvas: Canvas, left: Float, top: Float, widthPx: Int, heightPx: Int) {
        val label = "${(widthPx / density).toInt()}×${(heightPx / density).toInt()}"
        val pad = 2f * density
        val tw = sizeLabelText.measureText(label)
        val th = sizeLabelText.textSize
        canvas.drawRect(left, top, left + tw + pad * 2, top + th + pad * 2, sizeLabelBg)
        canvas.drawText(label, left + pad, top + th + pad, sizeLabelText)
    }

    private fun drawTouches(canvas: Canvas) {
        val showRadius = InterfaceToolkit.showTouchRadius.value
        val showDuration = InterfaceToolkit.showTouchDuration.value
        val base = 14f * density
        val fadeMs = 500L
        val now = System.currentTimeMillis()

        // Fading marks for lifted fingers.
        while (trails.isNotEmpty() && now - trails.first().at > fadeMs) trails.removeFirst()
        trails.forEach { t ->
            val progress = (now - t.at) / fadeMs.toFloat()
            val alpha = (150 * (1f - progress)).toInt().coerceIn(0, 255)
            touchPaint.color = (alpha shl 24) or TOUCH_RGB
            canvas.drawCircle(t.x, t.y, base * (1f + progress), touchPaint)
        }

        // Active fingers: a small spot that follows the finger, with optional radius + duration.
        activePointers.forEach { (id, p) ->
            touchPaint.color = (0x66 shl 24) or TOUCH_RGB
            canvas.drawCircle(p.x, p.y, base, touchPaint)
            touchRingPaint.color = (0xFF shl 24) or TOUCH_RGB
            canvas.drawCircle(p.x, p.y, base, touchRingPaint)
            if (showRadius) {
                // Use the device's real contact radius when it reports one; otherwise a
                // visible fallback ring (emulators / mouse input report no touch major).
                val radiusPx = pointerRadius[id] ?: 0f
                canvas.drawCircle(p.x, p.y, if (radiusPx > base) radiusPx else base * 2.4f, touchRingPaint)
            }
            if (showDuration) {
                val held = now - (pointerDownAt[id] ?: now)
                fpsText.color = (0xFF shl 24) or TOUCH_RGB
                canvas.drawText("$held ms", p.x + base * 2.6f, p.y + fpsText.textSize / 3f, fpsText)
            }
        }
    }

    private fun drawFps(canvas: Canvas) {
        val value = currentFps
        InterfaceToolkit.reportFps(value)
        val label = "FPS: $value"
        fpsText.color = when {
            value < FPS_POOR -> 0xFFFF5252.toInt()
            value < FPS_GOOD -> 0xFFFFC107.toInt()
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

    private companion object {
        /** Pink RGB for touch markers; alpha is applied per-draw. */
        const val TOUCH_RGB = 0xFF4081

        /** Fixed FPS colour thresholds, matching Scyther: <30 poor, 30–54 acceptable, 55+ good. */
        const val FPS_POOR = 30
        const val FPS_GOOD = 55
    }
}
