package kiol.vkapp.taskb.editor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.withClip
import kiol.vkapp.commonui.pxF
import kiol.vkapp.taskb.R
import kiol.vkapp.taskb.editor.VideoEditorTimebar.SelectedCutThumb.*
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min


class VideoEditorTimebar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onCutCallback: (left: Float, right: Float, inProcess: Boolean, activeThumb: SelectedCutThumb) -> Unit = { _, _, _, _ -> }

    enum class SelectedCutThumb {
        LEFT, RIGHT, NONE
    }

    private val RADIUS = 7.pxF
    private val PROGRESS_RADIUS_THUMB = 6.pxF
    private val PROGRESS_THUMB_OFFSET = 11.pxF
    private val PROGRESS_THUMB_WIDTH = 2.pxF

    private val BORDERSTROKE = 4.pxF

    private val CUT_THUMB_W = 12.pxF
    private val CUT_THUMB_STROKE = 3.pxF
    private val CUT_THUMB_TAP_SPACE = 3.pxF
    private val ARROW_STROKE = 2.pxF
    private val MIN_CUT_THRESHOLD = 0.1f

    private val ARROW_WIDTH = 7.pxF
    private val ARROW_HEIGHT = 10.pxF


    private val timebar: LinearLayout

    private val thumbAnim = ValueAnimator.ofInt().apply {
        duration = 150
        addUpdateListener {
            paintThumb.alpha = it.animatedValue as Int
            paintThumbCircle.alpha = it.animatedValue as Int
            invalidate()
        }
    }

    private val paintBorderStroke = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = BORDERSTROKE
        isAntiAlias = true
    }

    private val paintBorder = Paint().apply {
        color = 0xAAFFFFFF.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintThumb = Paint().apply {
        color = 0xFFFF3347.toInt()
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = PROGRESS_THUMB_WIDTH
    }

    private val paintThumbCircle = Paint().apply {
        color = 0xFFFF3347.toInt()
        isAntiAlias = true
    }

    private val paintThumbCut = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = 0xFF000000.toInt()
        strokeWidth = CUT_THUMB_STROKE
        isAntiAlias = true
    }

    private val paintArrow = Paint().apply {
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
        strokeWidth = ARROW_STROKE
        isAntiAlias = true
    }

    private var selectedCutThumb = NONE
    private var lastEventThumbX = 0f

    private var currentRelativePos = 0f

    private var flagCutChanged = false
    private var leftCut = 0.0f
    private var rightCut = 1.0f

    private var outerClipRect = Path()
    private var outerRect = RectF()
    private var cutThumbRect = Path()

    private var leftCutX = 0f
    private var rightCutX = 0f

    private var cutClipRect = RectF()

    init {
        val v = LayoutInflater.from(context).inflate(
            R.layout.video_editor_timebar_layout,
            this, true
        )
        timebar = v.findViewById(R.id.timebar)
        setWillNotDraw(false)
    }

    fun addThumbnail(image: Bitmap) {
        val imageView = ImageView(context)
        imageView.layoutParams = LinearLayout.LayoutParams(
            (timebar.measuredWidth) / 10, ViewGroup
                .LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageBitmap(image)
        timebar.addView(imageView)
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        outerClipRect = partlyRoundedRect(
            timebar.left.toFloat(), timebar.top.toFloat(), timebar.right.toFloat(),
            timebar.bottom.toFloat(), RADIUS, RADIUS, true, true, true, true
        )

        outerRect.set(
            timebar.left.toFloat(), timebar.top.toFloat(), timebar.right.toFloat(),
            timebar.bottom.toFloat()
        )

        updateDrawingPositions()
    }

    private fun updateDrawingPositions() {
        leftCutX = timebar.left.toFloat() + leftCut * timebar.measuredWidth
        rightCutX = timebar.left.toFloat() + rightCut * timebar.measuredWidth

        cutThumbRect = partlyRoundedRect(
            leftCutX - CUT_THUMB_W / 2,
            timebar.top.toFloat(),
            rightCutX + CUT_THUMB_W / 2
            ,
            timebar.bottom.toFloat(),
            RADIUS,
            RADIUS,
            true,
            true,
            true,
            true
        )

        cutClipRect.set(
            leftCutX + CUT_THUMB_W / 2,
            timebar.top.toFloat() + CUT_THUMB_STROKE,
            rightCutX - CUT_THUMB_W / 2,
            timebar
                .bottom.toFloat() - CUT_THUMB_STROKE
        )

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
                    val delta = (event.x - lastEventThumbX) / timebar.measuredWidth
                    lastEventThumbX = event.x
                    when (selectedCutThumb) {
                        LEFT -> {
                            if (leftCut + delta <= rightCut - MIN_CUT_THRESHOLD) {
                                leftCut += delta
                                leftCut = max(0.0f, leftCut)
                                setFlatCutChanging(true)
                            }
                        }
                        RIGHT -> {
                            if (rightCut + delta >= leftCut + MIN_CUT_THRESHOLD) {
                                rightCut += delta
                                rightCut = min(1.0f, rightCut)
                                setFlatCutChanging(true)
                            }
                        }
                    }
                    if (flagCutChanged) {
                        onCutCallback(leftCut, rightCut, true, selectedCutThumb)
                    }
                    updateDrawingPositions()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (flagCutChanged) {
                    onCutCallback(leftCut, rightCut, false, NONE)
                    setFlatCutChanging(false)
                }
                setNewSelectedCutThumb(NONE)
                updateDrawingPositions()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setFlatCutChanging(value: Boolean) {
        if (flagCutChanged != value) {
            flagCutChanged = value

            thumbAnim.cancel()
            if (flagCutChanged) {
                thumbAnim.setIntValues(255, 0)
            } else {
                thumbAnim.setIntValues(0, 255)
            }
            thumbAnim.start()
        }
    }

    private fun checkCutThumb(event: MotionEvent) {
        val ex = event.x
        val ey = event.y
        if (ey >= timebar.top && ey <= timebar.bottom) {
            val leftCutX = timebar.left.toFloat() + leftCut * timebar.measuredWidth
            val rightCutX = timebar.left.toFloat() + rightCut * timebar.measuredWidth

            if (ex >= leftCutX - CUT_THUMB_W / 2 - CUT_THUMB_TAP_SPACE
                && ex <= leftCutX + CUT_THUMB_W + CUT_THUMB_TAP_SPACE
            ) {
                setNewSelectedCutThumb(LEFT)
            } else if (ex >= rightCutX - CUT_THUMB_W / 2 - CUT_THUMB_TAP_SPACE
                && ex <= rightCutX + CUT_THUMB_W + CUT_THUMB_TAP_SPACE
            ) {
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
        canvas?.apply {
            withClip(outerClipRect) {
                super.dispatchDraw(canvas)

                withClipOutRect(cutClipRect) {
                    drawRect(outerRect, paintBorder)
                    drawPath(outerClipRect, paintBorderStroke)
                }
            }

            withClipOutRect(cutClipRect) {
                drawPath(cutThumbRect, paintThumbCut)
            }

            drawLeftArrow(canvas)
            drawRightArrow(canvas)
            drawProgressThumb(canvas)
        }
    }

    private fun drawLeftArrow(canvas: Canvas) {

        val arrowLeftStartPointX = leftCutX - ARROW_WIDTH / 2
        val arrowLeftStartPointY = outerRect.centerY()

        canvas.drawLine(
            arrowLeftStartPointX, arrowLeftStartPointY,
            arrowLeftStartPointX + ARROW_WIDTH / 2,
            arrowLeftStartPointY + ARROW_HEIGHT,
            paintArrow
        )
        canvas.drawLine(
            arrowLeftStartPointX,
            arrowLeftStartPointY,
            arrowLeftStartPointX + ARROW_WIDTH / 2,
            arrowLeftStartPointY - ARROW_HEIGHT,
            paintArrow
        )
    }

    private fun drawRightArrow(canvas: Canvas) {
        val arrowRightStartPointX = rightCutX + ARROW_WIDTH / 2
        val arrowRightStartPointY = outerRect.centerY()

        canvas.drawLine(
            arrowRightStartPointX, arrowRightStartPointY,
            arrowRightStartPointX - ARROW_WIDTH / 2,
            arrowRightStartPointY + ARROW_HEIGHT,
            paintArrow
        )
        canvas.drawLine(
            arrowRightStartPointX,
            arrowRightStartPointY,
            arrowRightStartPointX - ARROW_WIDTH / 2,
            arrowRightStartPointY - ARROW_HEIGHT,
            paintArrow
        )
    }

    private fun drawProgressThumb(canvas: Canvas) {
        val curPos = leftCutX + CUT_THUMB_W / 2 + (rightCutX - leftCutX - CUT_THUMB_W) * currentRelativePos
        canvas.drawLine(
            curPos,
            timebar.bottom.toFloat() - CUT_THUMB_STROKE,
            curPos,
            timebar.top.toFloat() - PROGRESS_THUMB_OFFSET, paintThumb
        )

        canvas.drawCircle(
            curPos,
            timebar.top.toFloat() - PROGRESS_THUMB_OFFSET,
            PROGRESS_RADIUS_THUMB, paintThumbCircle
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
        if (tr) path.rQuadTo(0f, -ry, -rx, -ry)
        else {
            path.rLineTo(0f, -ry)
            path.rLineTo(-rx, 0f)
        }
        path.rLineTo(-widthMinusCorners, 0f)
        if (tl) path.rQuadTo(-rx, 0f, -rx, ry)
        else {
            path.rLineTo(-rx, 0f)
            path.rLineTo(0f, ry)
        }
        path.rLineTo(0f, heightMinusCorners)
        if (bl) path.rQuadTo(0f, ry, rx, ry)
        else {
            path.rLineTo(0f, ry)
            path.rLineTo(rx, 0f)
        }
        path.rLineTo(widthMinusCorners, 0f)
        if (br) path.rQuadTo(rx, 0f, rx, -ry)
        else {
            path.rLineTo(rx, 0f)
            path.rLineTo(0f, -ry)
        }
        path.rLineTo(0f, -heightMinusCorners)
        path.close()
        return path
    }

    private inline fun Canvas.withClipOutRect(rect: RectF, block: Canvas.() -> Unit) {
        val checkpoint = save()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            clipOutRect(rect)
        } else {
            clipRect(rect, Region.Op.DIFFERENCE)
        }
        try {
            block()
        } finally {
            restoreToCount(checkpoint)
        }
    }
}