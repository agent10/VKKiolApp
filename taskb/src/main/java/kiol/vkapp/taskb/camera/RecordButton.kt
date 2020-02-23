package kiol.vkapp.taskb.camera

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withClip
import androidx.core.graphics.withScale
import kotlinx.android.synthetic.main.camera_container_fragment.*
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RecordButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val recClipPath = Path()

    private val progressButtonPaint = Paint().apply {
        color = 0xFFFF0000.toInt()
        isAntiAlias = true
    }

    private val progressButtonBackgroundPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        isAntiAlias = true

    }

    private val recButtonPaint = Paint().apply {
        color = 0xAAAAAAAA.toInt()
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

    var zoomHeight = 1.0f
        set(value) {
            field = max(1.0f, value)
        }

    var callback: (zoomLevel: Float) -> Unit = {}

    init {
        setWillNotDraw(false)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                setScale(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.y < 0) {
                    val zl = 3 * (abs(event.y) / zoomHeight)
                    Timber.d("Record button: $zl")
                    callback.invoke(zl)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setScale(false)
                return true
            }
        }
        return super.onTouchEvent(event)
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
                recClipPath.reset()
                recClipPath.addCircle(measuredWidth / 2f, measuredHeight / 2f, measuredWidth * 0.29f, Path.Direction.CCW)

                save()
                clipPath(recClipPath, Region.Op.DIFFERENCE)
                val r = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
                r.inset(measuredWidth * 0.15f, measuredHeight * 0.15f)
                drawArc(r, 0f, 360f, true, progressButtonBackgroundPaint)
                drawArc(r, 0f, 300f, true, progressButtonPaint)

                restore()

                drawCircle(measuredWidth / 2f, measuredHeight / 2f, measuredWidth * 0.27f, recButtonPaint)
            }
        }
    }
}