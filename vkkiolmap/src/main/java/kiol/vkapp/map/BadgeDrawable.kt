package kiol.vkapp.map

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import kiol.vkapp.commonui.pxF

class BadgeDrawable(context: Context?) : Drawable() {
    private val mTextSize = 13.pxF
    private val mBadgePaint: Paint = Paint().apply {
        color = 0xFF232324.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val mTextPaint: Paint
    private val mTxtRect = Rect()
    private var mCount = ""
    private var mWillDraw = false

    override fun draw(canvas: Canvas) {
        if (!mWillDraw) {
            return
        }
        val bounds = bounds
        val width = bounds.right - bounds.left.toFloat()
        val height = bounds.bottom - bounds.top.toFloat()

        val radius = 13.pxF
        val centerX = bounds.left.toFloat() + radius / 2f
        val centerY = 0f

        canvas.drawCircle(centerX, centerY, radius, mBadgePaint)

        mTextPaint.getTextBounds(mCount, 0, mCount.length, mTxtRect)
        val textHeight = mTxtRect.bottom - mTxtRect.top.toFloat()
        val textY = centerY + textHeight / 2f
        canvas.drawText(mCount, centerX, textY, mTextPaint)
    }

    /*
    Sets the count (i.e notifications) to display.
     */
    fun setCount(count: Int) {
        mCount = Integer.toString(count)

        // Only draw a badge if there are notifications.
        mWillDraw = count > 0
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        // do nothing
    }

    override fun setColorFilter(cf: ColorFilter?) {
        // do nothing
    }

    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }

    init {
        mTextPaint = Paint()
        mTextPaint.color = 0xFFE1E3E6.toInt()
        mTextPaint.typeface = Typeface.DEFAULT_BOLD
        mTextPaint.textSize = mTextSize
        mTextPaint.isAntiAlias = true
        mTextPaint.textAlign = Paint.Align.CENTER
    }
}