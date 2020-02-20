package kiol.vkapp.taskb.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.withClip
import kiol.vkapp.taskb.R
import kiol.vkapp.taskb.editor.VideoEditorTimebar.SelectedCutThumb.*
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min


class VideoEditorTimebar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onCutCallback: (left: Float, right: Float, inProcess: Boolean) -> Unit = { _, _, _ -> }

    private enum class SelectedCutThumb {
        LEFT, RIGHT, NONE
    }

    companion object {
        private const val RADIUS = 40f
        private const val RADIUS_POS_THUMB = 20f
        private const val POS_THUMB_OFFSET = 30f
        private const val BORDERSTROKE = 10f
        private const val CUT_THUMB_W = 50f
        private const val CUT_THUMB_STROKE = 15f
        private const val MIN_CUT_THRESHOLD = 0.1f
    }

    private val linearLayout: LinearLayout

    private val paintBorderStroke = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = BORDERSTROKE
    }

    private val paintBorder = Paint().apply {
        color = 0xAAFFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private val paintThumb = Paint().apply {
        color = 0xFFFF0000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val paintThumbCircle = Paint().apply {
        color = 0xFFFF0000.toInt()
    }

    private val paintThumbCut = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = 0xFF000000.toInt()
        strokeWidth = CUT_THUMB_STROKE
    }

    private var selectedCutThumb = NONE
    private var lastEventThumbX = 0f

    private var currentRelativePos = 0f

    private var flagCutChanged = false
    private var leftCut = 0.0f
    private var rightCut = 1.0f


    init {
        val v = LayoutInflater.from(context).inflate(R.layout.video_editor_timebar_layout, this, true)
        linearLayout = v.findViewById(R.id.timebar)
        setWillNotDraw(false)
    }

    fun addThumbnail(image: Bitmap) {
        val imageView = ImageView(context)
        imageView.layoutParams = LinearLayout.LayoutParams(
            (linearLayout.measuredWidth) / 10, ViewGroup
                .LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageBitmap(image)
        linearLayout.addView(imageView)
    }

    fun setPosition(position: Long, duration: Long) {
        if (duration != 0L) {
            currentRelativePos = position.toFloat() / duration.toFloat()
            invalidate()
        }
    }

    fun setPosition(relativePos: Float) {
        currentRelativePos = relativePos
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                checkCutThumb(event)
                if (selectedCutThumb != NONE) {
                    lastEventThumbX = event.x
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedCutThumb != NONE) {
                    val delta = (event.x - lastEventThumbX) / linearLayout.measuredWidth
                    lastEventThumbX = event.x
                    when (selectedCutThumb) {
                        LEFT -> {
                            if (leftCut + delta <= rightCut - MIN_CUT_THRESHOLD) {
                                leftCut += delta
                                leftCut = max(0.0f, leftCut)
                                flagCutChanged = true
                            }
                        }
                        RIGHT -> {
                            if (rightCut + delta >= leftCut + MIN_CUT_THRESHOLD) {
                                rightCut += delta
                                rightCut = min(1.0f, rightCut)
                                flagCutChanged = true

                            }
                        }
                    }
                    if (flagCutChanged) {
                        onCutCallback(leftCut, rightCut, true)
                    }
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (flagCutChanged) {
                    onCutCallback(leftCut, rightCut, false)
                    flagCutChanged = false
                }
                setNewSelectedCutThumb(NONE)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun checkCutThumb(event: MotionEvent) {
        val ex = event.x
        val ey = event.y
        if (ey >= linearLayout.top && ey <= linearLayout.bottom) {
            val leftCutX = linearLayout.left.toFloat() + leftCut * linearLayout.measuredWidth
            val rightCutX = linearLayout.left.toFloat() + rightCut * linearLayout.measuredWidth

            if (ex >= leftCutX - CUT_THUMB_W / 2 && ex <= leftCutX + CUT_THUMB_W) {
                setNewSelectedCutThumb(LEFT)
            } else if (ex >= rightCutX - CUT_THUMB_W / 2 && ex <= rightCutX + CUT_THUMB_W) {
                setNewSelectedCutThumb(RIGHT)
            } else {
                setNewSelectedCutThumb(NONE)
            }
        } else {
            setNewSelectedCutThumb(NONE)
        }
    }

    private fun setNewSelectedCutThumb(thumb: SelectedCutThumb) {
        selectedCutThumb = thumb
        Timber.d("selectedCutThumb: $selectedCutThumb")
    }

    override fun dispatchDraw(canvas: Canvas?) {
        val outerRect = partlyRoundedRect(
            linearLayout.left.toFloat(), linearLayout.top.toFloat(), linearLayout.right.toFloat()
            , linearLayout.bottom.toFloat(), RADIUS, RADIUS, true, true, true, true
        )

        val leftCutX = linearLayout.left.toFloat() + leftCut * linearLayout.measuredWidth
        val rightCutX = linearLayout.left.toFloat() + rightCut * linearLayout.measuredWidth

        val cutThumbRect = partlyRoundedRect(
            leftCutX - CUT_THUMB_W / 2,
            linearLayout.top.toFloat(),
            rightCutX + CUT_THUMB_W / 2
            ,
            linearLayout.bottom.toFloat(),
            RADIUS,
            RADIUS,
            true,
            true,
            true,
            true
        )

        val cutClipRect = RectF(
            leftCutX + CUT_THUMB_W / 2, linearLayout.top.toFloat() + CUT_THUMB_STROKE, rightCutX - CUT_THUMB_W / 2,
            linearLayout
                .bottom.toFloat() - CUT_THUMB_STROKE
        )

        canvas?.apply {
            withClip(outerRect) {
                super.dispatchDraw(canvas)

                save()
                clipRect(cutClipRect, Region.Op.DIFFERENCE)
                drawPath(outerRect, paintBorder)
                drawPath(outerRect, paintBorderStroke)
                restore()
            }

            save()
            clipRect(cutClipRect, Region.Op.DIFFERENCE)
            drawPath(cutThumbRect, paintThumbCut)
            restore()

            if(!flagCutChanged) {
                val curPos = leftCutX + CUT_THUMB_W / 2 + (rightCutX - leftCutX - CUT_THUMB_W) * currentRelativePos
                drawLine(
                    curPos,
                    linearLayout.bottom.toFloat(),
                    curPos,
                    linearLayout.top.toFloat() - POS_THUMB_OFFSET, paintThumb
                )

                drawCircle(
                    curPos,
                    linearLayout.top.toFloat() - POS_THUMB_OFFSET,
                    RADIUS_POS_THUMB, paintThumbCircle
                )
            }
        }
    }

    private fun partlyRoundedRect(
        left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float,
        tl: Boolean, tr: Boolean, br: Boolean, bl: Boolean
    ): Path {
        var rx = rx
        var ry = ry
        val path = Path()
        if (rx < 0) rx = 0f
        if (ry < 0) ry = 0f
        val width = right - left
        val height = bottom - top
        if (rx > width / 2) rx = width / 2
        if (ry > height / 2) ry = height / 2
        val widthMinusCorners = width - 2 * rx
        val heightMinusCorners = height - 2 * ry
        path.moveTo(right, top + ry)
        if (tr) path.rQuadTo(0f, -ry, -rx, -ry) //top-right corner
        else {
            path.rLineTo(0f, -ry)
            path.rLineTo(-rx, 0f)
        }
        path.rLineTo(-widthMinusCorners, 0f)
        if (tl) path.rQuadTo(-rx, 0f, -rx, ry) //top-left corner
        else {
            path.rLineTo(-rx, 0f)
            path.rLineTo(0f, ry)
        }
        path.rLineTo(0f, heightMinusCorners)
        if (bl) path.rQuadTo(0f, ry, rx, ry) //bottom-left corner
        else {
            path.rLineTo(0f, ry)
            path.rLineTo(rx, 0f)
        }
        path.rLineTo(widthMinusCorners, 0f)
        if (br) path.rQuadTo(rx, 0f, rx, -ry) //bottom-right corner
        else {
            path.rLineTo(rx, 0f)
            path.rLineTo(0f, -ry)
        }
        path.rLineTo(0f, -heightMinusCorners)
        path.close() //Given close, last lineto can be removed.
        return path
    }
}