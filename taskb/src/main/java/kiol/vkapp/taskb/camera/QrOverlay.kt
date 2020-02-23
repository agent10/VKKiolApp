package kiol.vkapp.taskb.camera

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withRotation
import kiol.vkapp.commonui.dpF
import kiol.vkapp.commonui.pxF
import kiol.vkapp.taskb.CameraFragment
import timber.log.Timber
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt




data class QrDrawModel(val x: Float, val y: Float, val size: Float, val angle: Float)

class QrOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        val EmptyQrDrawModel = QrDrawModel(0f, 0f, 0f, 0f)
    }

    private var isShowing = false

    private var animScale = 0f
    private val scaleAnimator = ValueAnimator.ofFloat(0.0f, 10.0f).apply {
        repeatMode = ValueAnimator.REVERSE
        repeatCount = Int.MAX_VALUE / 2
        duration = 500
        addUpdateListener {
            animScale = it.animatedValue as Float
            invalidate()
        }
        start()
    }

    private var alphaValue = 0f
    private val alphaAnimator = ValueAnimator.ofFloat().apply {
        duration = 500
        addUpdateListener {
            alphaValue = it.animatedValue as Float
            invalidate()
        }
    }

    private var rectSize = 0f
    private val rectSizeAnimator = ValueAnimator.ofFloat().apply {
        duration = 50
        addUpdateListener {

            rectSize = it.animatedValue as Float
            invalidate()
        }
    }

    private var angle = 0f
    private val angleAnimator = ValueAnimator.ofFloat().apply {
        duration = 50
        addUpdateListener {
            angle = it.animatedValue as Float
            invalidate()
        }
    }

    private val centerPoint = PointF()
    private val centerAnimatorX = ValueAnimator.ofFloat().apply {
        duration = 50
        addUpdateListener {
            centerPoint.x = it.animatedValue as Float
            invalidate()
        }
    }

    private val centerAnimatorY = ValueAnimator.ofFloat().apply {
        duration = 50
        addUpdateListener {
            centerPoint.y = it.animatedValue as Float
            invalidate()
        }
    }


    init {
        setWillNotDraw(false)
    }

    private var lastQrResult: CameraFragment.QrMyResult? = null
    private var qrDrawModel: QrDrawModel? = null

    private val qrRect = RectF()

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
        strokeWidth = 5.pxF
    }

    fun drawQr2(qrRes: CameraFragment.QrMyResult) {
        lastQrResult = qrRes

        invalidate()
    }

    fun drawQr(qrDrawModel: QrDrawModel) {
        this.qrDrawModel = qrDrawModel
        centerAnimatorX.cancel()
        centerAnimatorY.cancel()
        rectSizeAnimator.cancel()
        angleAnimator.cancel()

        val toShow = qrDrawModel.size > 0.1f
        if (toShow) {
            centerAnimatorX.setFloatValues(centerPoint.x, qrDrawModel.x)
            centerAnimatorY.setFloatValues(centerPoint.y, qrDrawModel.y)
            rectSizeAnimator.setFloatValues(rectSize, qrDrawModel.size)
            angleAnimator.setFloatValues(angle, if (qrDrawModel.angle < 0) 360f + qrDrawModel.angle else qrDrawModel.angle)

            centerAnimatorX.start()
            centerAnimatorY.start()
            rectSizeAnimator.start()
            angleAnimator.start()
        }

        if (isShowing && !toShow) {
            alphaAnimator.cancel()
            alphaAnimator.setFloatValues(alphaValue, 0.0f)
            alphaAnimator.start()
            isShowing = false
        } else if (!isShowing && toShow) {
            alphaAnimator.cancel()
            alphaAnimator.setFloatValues(alphaValue, 1.0f)
            alphaAnimator.start()
            isShowing = true
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            qrDrawModel?.let {

                qrRect.set(centerPoint.x, centerPoint.y, centerPoint.x, centerPoint.y)
                qrRect.inset(-rectSize + animScale, -rectSize + animScale)
                paint2.alpha = (255 * alphaValue).toInt()

                withRotation(angle, qrRect.centerX(), qrRect.centerY()) {
                    drawRoundRect(qrRect, 30f, 30f, paint2)
                }
            }
        }
    }
}