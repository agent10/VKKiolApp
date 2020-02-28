package kiol.vkapp.taskb.camera.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.shapes.ArcShape
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.graphics.withScale
import kiol.vkapp.commonui.pxF
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max

class RecordButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Callback {
        fun onZoomLevel(zoomLevel: Float)
        fun onRecord(started: Boolean)
    }

    private val recButtonPaint = Paint().apply {
        color = 0xAAFFFFFF.toInt()
        isAntiAlias = true
    }

    private val progressButtonPaint = Paint().apply {
        color = 0xFFFF0000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 7.pxF
        isAntiAlias = true
    }

    private val progressButtonBackgroundPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 7.pxF
        isAntiAlias = true

    }

    private var scaleValue = 1.0f
    private val scaleAnimator = ValueAnimator.ofFloat().apply {
        duration = 100
        addUpdateListener {
            scaleValue = it.animatedValue as Float
            invalidate()
        }
    }

    private var progressValue = 0f
    private val progressAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 1000 * 15
        addUpdateListener {
            progressValue = it.animatedValue as Float
            invalidate()
        }
        doOnEnd {
            handleStopRecording()
        }
    }

    var zoomHeight = 1.0f
        set(value) {
            field = max(1.0f, value)
        }

    var callback: Callback = object :
        Callback {
        override fun onZoomLevel(zoomLevel: Float) {}
        override fun onRecord(started: Boolean) {}
    }

    private val progressRect = RectF()

    init {
        setWillNotDraw(false)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        progressRect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        progressRect.inset(measuredWidth * 0.15f, measuredHeight * 0.15f)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                progressAnimator.cancel()
                progressAnimator.start()
                callback.onRecord(true)
                setScale(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.y < 0) {
                    val zl = 3 * (abs(event.y) / zoomHeight)
                    Timber.d("Record button: $zl")
                    callback.onZoomLevel(zl)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                progressAnimator.cancel()
                progressValue = 0.0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleStopRecording() {
        callback.onRecord(false)
        setScale(false)
    }

    private fun setScale(value: Boolean) {
        scaleAnimator.cancel()
        if (value) {
            scaleAnimator.setFloatValues(scaleValue, 1.15f)
        } else {
            scaleAnimator.setFloatValues(scaleValue, 1.0f)
        }
        scaleAnimator.start()
        invalidate()
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            withScale(scaleValue, scaleValue, measuredWidth / 2f, measuredHeight / 2f) {
                drawArc(progressRect, 0f, 360f, true, progressButtonBackgroundPaint)
                drawArc(progressRect, -90f, progressValue, false, progressButtonPaint)
                drawCircle(measuredWidth / 2f, measuredHeight / 2f, measuredWidth * 0.30f, recButtonPaint)
            }
        }
    }
}