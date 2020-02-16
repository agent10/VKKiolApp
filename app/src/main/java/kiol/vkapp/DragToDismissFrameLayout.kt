package kiol.vkapp

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs
import kotlin.math.max

class DragToDismissFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var dissmissHandler: () -> Unit = {}

    private var lastDownY = 0f
    private var totalScroll = 0f
    private var lastKoef = 0.0f

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastDownY = event.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffY = event.y - lastDownY

                    totalScroll += diffY
                    lastKoef = (abs(totalScroll) / (measuredHeight / 2))
                    y += event.y - lastDownY
                    alpha = max(1.0f - lastKoef, 0.1f)

                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(lastKoef) <= 0.8f) {
                        clearAnimation()
                        animate().alpha(1.0f).y(0f).setInterpolator(OvershootInterpolator()).duration =
                            250
                    } else {
                        dissmissHandler.invoke()
                    }
                    totalScroll = 0f
                    lastKoef = 0f
                }
            }
        }

        return super.onTouchEvent(event)
    }
}