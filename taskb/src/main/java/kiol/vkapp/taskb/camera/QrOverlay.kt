package kiol.vkapp.taskb.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withScale
import com.google.zxing.ResultPoint
import timber.log.Timber
import java.lang.Math.asin
import kotlin.math.*


class Vector(x1: Float, y1: Float, x2: Float, y2: Float) {
    val vec = PointF(x2 - x1, y2 - y1)
    val len = sqrt(vec.x.pow(2) + vec.y.pow(2))
}

fun scalar(v1: Vector, v2: Vector): Float {
    val t = v1.vec.x * v2.vec.x + v1.vec.y * v2.vec.y
    val g = v1.len * v2.len
    return t / g
}

class QrOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
    }

    private var lastRect: RectF? = null
    private var lastRects: List<ResultPoint>? = null
    private var cosa = 0f

    private var koefX = 0f
    private var koefY = 0f

    private val paint = Paint().apply {
        color = Color.RED
    }

    private val paintDebug = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val paintDebug2 = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val paint2 = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }

    fun drawQr(points: List<ResultPoint>, kX: Float, kY: Float, angle: Float) {
        if (points.size == 4) {
            lastRects = points.map {
                ResultPoint(it.x, it.y)
            }

            koefX = kX
            koefY = kY


            var xa = lastRects!!.sumByDouble {
                it.x.toDouble()
            }.toFloat() / 4f

            var ya = lastRects!!.sumByDouble {
                it.y.toDouble()
            }.toFloat() / 4f

            var r = sqrt((xa - lastRects!![0].x).pow(2) + (ya - lastRects!![0].y).pow(2))

            lastRect = RectF(xa - r, ya - r, xa + r, ya + r)

            val rv = Vector(xa, ya, lastRect!!.right, lastRect!!.top)
            val lv = Vector(xa, ya, lastRects!![0].x, lastRects!![0].y)

            val v = acos(scalar(rv, lv))
            val z = rv.vec.x * lv.vec.y - rv.vec.y * lv.vec.x

            Timber.d("scalar: $v")
            cosa = v * sign(z)
        } else {
            lastRects = null
            lastRect = null
        }


        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            lastRects?.forEach {
                drawCircle(it.x, it.y, 5f, paint)
            }

            lastRect?.let {
                canvas.save()

                val angle = cosa * 180 / Math.PI.toFloat()
                Timber.d("angle $angle")

                canvas.translate(-it.centerX() * (1f - koefX), -it.centerY() * (1f - koefY))
                val s = max(koefX, koefY)
                canvas.scale(s, s, it.centerX(), it.centerY())
                canvas.rotate(angle, it.centerX(), it.centerY())

                drawRect(it, paint2)
                canvas.restore()

                drawLine(it.centerX(), it.centerY(), it.centerX() * koefX, it.centerY() * (koefY), paintDebug)
                //                drawLine(it.centerX(), it.centerY(), lastRects!![2].x, lastRects!![2].y, paintDebug2)

            }
        }
    }
}