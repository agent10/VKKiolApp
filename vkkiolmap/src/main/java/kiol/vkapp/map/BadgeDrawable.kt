package kiol.vkapp.map

import android.graphics.*
import android.graphics.drawable.Drawable
import kiol.vkapp.commonui.pxF

class BadgeDrawable() : Drawable() {
    private val textSize = 13.pxF
    private val badgePaint: Paint = Paint().apply {
        color = 0xFF232324.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = 0xFFE1E3E6.toInt()
        typeface = Typeface.DEFAULT_BOLD
        textSize = textSize
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val txtRect = Rect()
    private var count = ""
    private var willDraw = false

    override fun draw(canvas: Canvas) {
        if (!willDraw) {
            return
        }
        val bounds = bounds

        val radius = 13.pxF
        val centerX = bounds.left.toFloat() + radius / 2f
        val centerY = 0f

        canvas.drawCircle(centerX, centerY, radius, badgePaint)

        textPaint.getTextBounds(count, 0, count.length, txtRect)
        val textHeight = txtRect.bottom - txtRect.top.toFloat()
        val textY = centerY + textHeight / 2f
        canvas.drawText(count, centerX, textY, textPaint)
    }

    fun setCount(count: Int) {
        this.count = Integer.toString(count)

        willDraw = count > 0
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        // do nothing
    }

    override fun setColorFilter(cf: ColorFilter?) {
        // do nothing
    }

    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }
}