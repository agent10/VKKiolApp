package kiol.vkapp.cam.camera.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRect
import androidx.core.graphics.withRotation
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import kiol.vkapp.cam.R


data class QrDrawModel(val x: Float, val y: Float, val size: Float, val angle: Float)

class QrOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        val EmptyQrDrawModel = QrDrawModel(0f, 0f, 0f, 0f)
        private const val QRRectScale = 1.2f
        private const val ScaleDuration = 500L
        private const val AlphaDuration = 500L
        private const val Duration = 50L
    }

    private var isShowing = false

    private var animScale = 0f
    private val scaleAnimator = ValueAnimator.ofFloat(0.0f, 10.0f).apply {
        repeatMode = ValueAnimator.REVERSE
        repeatCount = Int.MAX_VALUE / 2
        duration = ScaleDuration
        addUpdateListener {
            animScale = it.animatedValue as Float
            invalidate()
        }
    }

    private var alphaValue = 0f
    private val alphaAnimator = ValueAnimator.ofFloat().apply {
        duration = AlphaDuration
        addUpdateListener {
            alphaValue = it.animatedValue as Float
            invalidate()
        }
    }

    private var rectSize = 0f
    private val rectSizeAnimator = ValueAnimator.ofFloat().apply {
        duration = Duration
        addUpdateListener {

            rectSize = it.animatedValue as Float
            invalidate()
        }
    }

    private var angle = 0f
    private val angleAnimator = ValueAnimator.ofFloat().apply {
        duration = Duration
        addUpdateListener {
            angle = it.animatedValue as Float
            invalidate()
        }
    }

    private val centerPoint = PointF()
    private val centerAnimatorX = ValueAnimator.ofFloat().apply {
        duration = Duration
        addUpdateListener {
            centerPoint.x = it.animatedValue as Float
            invalidate()
        }
    }

    private val centerAnimatorY = ValueAnimator.ofFloat().apply {
        duration = Duration
        addUpdateListener {
            centerPoint.y = it.animatedValue as Float
            invalidate()
        }
    }

    private val qrBoundsDrawable: VectorDrawableCompat?

    init {
        setWillNotDraw(false)

        qrBoundsDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_cam_qr_bounds, null)
    }

    private var qrDrawModel: QrDrawModel? = null

    private val qrRect = RectF()

    fun drawQr(qrDrawModel: QrDrawModel) {
        if (this.qrDrawModel != qrDrawModel) {
            this.qrDrawModel = qrDrawModel
            centerAnimatorX.cancel()
            centerAnimatorY.cancel()
            rectSizeAnimator.cancel()
            angleAnimator.cancel()

            val toShow = qrDrawModel.size > 0.1f
            if (toShow) {
                centerAnimatorX.setFloatValues(centerPoint.x, qrDrawModel.x)
                centerAnimatorY.setFloatValues(centerPoint.y, qrDrawModel.y)
                rectSizeAnimator.setFloatValues(rectSize, qrDrawModel.size * QRRectScale)

                var newAngle = if (qrDrawModel.angle < 0) 360f + qrDrawModel.angle else qrDrawModel.angle
                if (newAngle.isNaN()) newAngle = 0f
                if (angle.isNaN()) angle = 0f
                angleAnimator.setFloatValues(angle, newAngle)

                centerAnimatorX.start()
                centerAnimatorY.start()
                rectSizeAnimator.start()
                angleAnimator.start()
            }

            if (isShowing && !toShow) {
                scaleAnimator.cancel()
                alphaAnimator.cancel()
                alphaAnimator.setFloatValues(alphaValue, 0.0f)
                alphaAnimator.start()
                isShowing = false
            } else if (!isShowing && toShow) {
                scaleAnimator.start()
                alphaAnimator.cancel()
                alphaAnimator.setFloatValues(alphaValue, 1.0f)
                alphaAnimator.start()
                isShowing = true
            }

            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (alphaValue > 0.0f) {
            canvas?.apply {
                qrDrawModel?.let {
                    qrBoundsDrawable?.let {

                        qrRect.set(centerPoint.x, centerPoint.y, centerPoint.x, centerPoint.y)
                        qrRect.inset(-rectSize + animScale, -rectSize + animScale)

                        it.bounds = qrRect.toRect()
                        it.alpha = (255 * alphaValue).toInt()

                        withRotation(angle, qrRect.centerX(), qrRect.centerY()) {
                            it.draw(canvas)
                        }
                    }
                }
            }
        }
    }
}