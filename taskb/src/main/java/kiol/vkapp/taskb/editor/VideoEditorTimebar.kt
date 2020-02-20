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

    private enum class SelectedCutThumb {
        LEFT, RIGHT, NONE
    }

    companion object {
        private const val RADIUS = 40f
        private const val BORDERSTROKE = 10f
        private const val CUT_THUMB_W = 75f
        private const val CUT_THUMB_STROKE = 15f
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

    private var leftCut = 0.25f
    private var rightCut = 0.75f

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
                            leftCut += delta
                            leftCut = max(0.0f, leftCut)
                        }
                        RIGHT -> {
                            rightCut += delta
                            rightCut = min(1.0f, rightCut)
                        }
                    }
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
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
        }


        //        canvas?.drawRoundRect(
        //            linearLayout.left.toFloat(),
        //            linearLayout.top.toFloat(),
        //            leftCut + 10, linearLayout.bottom.toFloat(), 20f, 20f, paintBorder
        //        )
        //
        //        canvas?.drawRoundRect(
        //            linearLayout.left.toFloat(),
        //            linearLayout.top.toFloat(),
        //            leftCut + 10, linearLayout.bottom.toFloat(), 20f, 20f, paintBorderStroke
        //        )
        //
        //        canvas?.drawRoundRect(
        //            rightCut,
        //            linearLayout.top.toFloat(),
        //            linearLayout.right.toFloat(), linearLayout.bottom.toFloat(), 20f, 20f, paintBorder
        //        )
        //
        //        canvas?.drawRoundRect(
        //            rightCut,
        //            linearLayout.top.toFloat(),
        //            linearLayout.right.toFloat(), linearLayout.bottom.toFloat(), 20f, 20f, paintBorderStroke
        //        )
        //
        //        drawCut(canvas!!)
        //
        //        val curPos = linearLayout.left + currentRelativePos * (linearLayout.measuredWidth).toFloat()
        //        canvas?.drawLine(
        //            curPos,
        //            linearLayout.bottom.toFloat(),
        //            curPos,
        //            linearLayout.top.toFloat() - 20f, paintThumb
        //        )
        //
        //        canvas?.drawCircle(
        //            curPos,
        //            linearLayout.top.toFloat() - 20f,
        //            20f, paintThumbCircle
        //        )
    }

    private fun drawCut(canvas: Canvas) {
        canvas.drawPath(
            partlyRoundedRect(
                leftCut,
                linearLayout.top.toFloat() - 5,
                leftCut + 100f, linearLayout.bottom.toFloat() + 5, 20f, 20f, true, false, false, true
            ), paintThumbCut
        )

        canvas.drawPath(
            partlyRoundedRect(
                rightCut,
                linearLayout.top.toFloat() - 5,
                rightCut + 100f, linearLayout.bottom.toFloat() + 5, 20f, 20f, false, true, true, false
            ), paintThumbCut
        )

        canvas.drawLine(
            leftCut + 20,
            linearLayout.top.toFloat() - 5,
            rightCut,
            linearLayout.top.toFloat() - 5,
            paintThumbCut
        )
        canvas.drawLine(
            leftCut + 20, linearLayout.bottom.toFloat() + 5, rightCut, linearLayout.bottom.toFloat() + 5,
            paintThumbCut
        )
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