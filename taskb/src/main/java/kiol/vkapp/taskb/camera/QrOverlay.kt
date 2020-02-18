package kiol.vkapp.taskb.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.zxing.ResultPoint

class QrOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
    }

    private var lastRect: Rect? = null
    private var lastRects: List<ResultPoint>? = null

    private val paint = Paint().apply {
        color = Color.RED
    }

    fun drawQr(points: List<ResultPoint>) {
        lastRects = points

        //        if (points.size == 4) {
        //            lastRect = Rect(points[0].x.toInt(), points[0].y.toInt(), points[0].x.toInt() + 20, points[0].y.toInt() + 20)
        //        } else {
        //            lastRect = null
        //        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (lastRects != null) {
            lastRects!!.forEach {
                canvas?.drawCircle(it.x, it.y, 20f, paint)
            }
        }
    }
}