package com.example.tennisballtracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

/**
 * Overlay view that draws the tracked tennis ball and a small trailing path
 * for a more dynamic look.
 */
class BallOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val ballPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 0f, Color.GREEN)
    }

    private val trailPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 160
    }

    private val trail = ArrayList<PointF>()
    private var position: PointF? = null

    init {
        // Enable shadow layer drawing
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * Update the ball position and redraw the overlay.
     */
    fun update(point: PointF?) {
        point?.let {
            trail.add(it)
            if (trail.size > 20) trail.removeAt(0)
        }
        position = point
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in 1 until trail.size) {
            val p0 = trail[i - 1]
            val p1 = trail[i]
            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, trailPaint)
        }

        position?.let {
            canvas.drawCircle(it.x, it.y, 30f, ballPaint)
        }
    }
}

