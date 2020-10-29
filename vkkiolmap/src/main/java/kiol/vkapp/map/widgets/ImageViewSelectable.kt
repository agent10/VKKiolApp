package kiol.vkapp.map.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import kiol.vkapp.commonui.pxF
import kotlin.math.cos
import kotlin.math.min

class ImageViewSelectable @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private val paintInner1 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = 0xFFE1E3E6.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4.pxF
    }

    private val paintInner2 = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = 0xFF232324.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.pxF
    }

    private val paintCircleBg = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = 0xFFE1E3E6.toInt()
    }

    private val paintCheck = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = 0xFF232324.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.pxF
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (isSelected) {
            val minSize = min(measuredWidth, measuredHeight)
            val radius = minSize / 2f
            canvas?.run {
                drawCircle(radius, radius, radius - 2.pxF, paintInner1)

                drawCircle(radius, radius, radius - 3.pxF, paintInner2)

                val cx = (radius + radius * cos(Math.PI / 4f)).toFloat()
                val cy = (radius + radius * kotlin.math.sin(Math.PI / 4f)).toFloat()
                drawCircle(cy, cy, 8.pxF, paintCircleBg)

                drawCheck(cx, cy, this)
            }
        }
    }

    private fun drawCheck(cx: Float, cy: Float, canvas: Canvas) {
        val arrowLeftStartPointX = cx - 5.pxF
        val arrowLeftStartPointY = cy - 2.pxF

        canvas.drawLine(
            arrowLeftStartPointX, arrowLeftStartPointY,
            cx - 2.pxF,
            cy + 2.pxF,
            paintCheck
        )
        canvas.drawLine(
            cx - 2.pxF,
            cy + 2.pxF,
            cx + 3.pxF,
            cy - 2.pxF,
            paintCheck
        )
    }
}