package kiol.vkapp.taskb.camera

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withScale
import com.google.zxing.ResultPoint
import kiol.vkapp.taskb.CameraFragment
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

    private var animV = 0f
    private val animator = ValueAnimator.ofFloat(0.0f, 0.2f).apply {
        repeatMode = ValueAnimator.REVERSE
        repeatCount = Int.MAX_VALUE / 2
        duration = 200
        addUpdateListener {
            animV = it.animatedValue as Float
        }
        start()
    }

    init {
        setWillNotDraw(false)
    }

    private var lastQrResult: CameraFragment.QrMyResult? = null

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

    fun drawQr2(qrRes: CameraFragment.QrMyResult) {
        lastQrResult = qrRes

        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            lastQrResult?.let { qrres ->
                val rect = qrres.rect

                rect?.let {
                    canvas.save()

                    val angle = qrres.angle
                    Timber.d("angle $angle")

                    canvas.translate(-it.centerX() * (1f - qrres.kX), -it.centerY() * (1f - qrres.kY))
                    val s = max(qrres.kX, qrres.kY) + animV
                    canvas.scale(s, s, it.centerX(), it.centerY())
                    canvas.rotate(angle, it.centerX(), it.centerY())

                    drawRect(it, paint2)
                    canvas.restore()
                }

            }
        }
    }
}