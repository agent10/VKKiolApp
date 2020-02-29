package kiol.vkapp.taskb.camera.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.shapes.ArcShape
import android.media.MediaActionSound
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

    companion object {
        private const val ScaleDuration = 100L
        private const val MaxRecordDuration = 15000L
    }

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
        duration = ScaleDuration
        addUpdateListener {
            scaleValue = it.animatedValue as Float
            invalidate()
        }
    }

    private var progressValue = 0f
    private val progressAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = MaxRecordDuration
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

    private lateinit var sound: MediaActionSound

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sound = MediaActionSound()
        sound.load(MediaActionSound.START_VIDEO_RECORDING)
        sound.load(MediaActionSound.STOP_VIDEO_RECORDING)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sound.release()
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
                playBeepSound(true)
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
                playBeepSound(false)
                progressAnimator.cancel()
                progressValue = 0.0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun playBeepSound(start: Boolean) {
        sound.play(if (start) MediaActionSound.START_VIDEO_RECORDING else MediaActionSound.STOP_VIDEO_RECORDING)
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