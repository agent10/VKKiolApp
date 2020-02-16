package kiol.vkapp

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewPropertyAnimator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max

class DragToDismissFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var dissmissHandler: () -> Unit = {}

    private var lastDownY = 0f

    private var lastAnim: ViewPropertyAnimator? = null

    private var blockTouhes = false

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        Timber.d("kiol onInterceptTouchEvent, $event")

        if (alpha < 1.0) {
            return true
        }

        event?.let {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    handleFirstTouch(event)
                }
                MotionEvent.ACTION_MOVE -> return true
            }
        }

        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Timber.d("kiol onTouchEvent, $event")
        event?.let {
            Timber.d("kiol onTouchEvent event.action, ${event.action}")
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!blockTouhes) {
                        handleFirstTouch(event)
                    }
                    return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (!blockTouhes) {
                        blockTouhes = true
                        restart()
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!blockTouhes) {
                        y += event.y - lastDownY
                        setAlphaValue()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    cancelAnim()
                    blockTouhes = false

                    if (alpha > 0.2f) {
                        restart()
                    } else {
                        dissmissHandler.invoke()
                    }
                    return true
                }
            }

            return false
        }

        return super.onTouchEvent(event)
    }

    private fun handleFirstTouch(event: MotionEvent) {
        cancelAnim()
        lastDownY = event.y

        setAlphaValue()
    }

    private fun restart() {
        lastAnim = animate().alpha(1.0f).y(0f).setInterpolator(OvershootInterpolator()).apply {
            duration = 250
        }
    }

    private fun setAlphaValue() {
        alpha = max(1.0f - abs(y) / (measuredHeight / 2), 0.1f)
    }

    private fun cancelAnim() {
        lastAnim?.cancel()
        lastAnim = null
    }
}