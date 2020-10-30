package kiol.vkapp.map.renderers.transformations

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.applyCanvas
import coil.bitmap.BitmapPool
import coil.size.Size
import coil.transform.Transformation
import kiol.vkapp.commonui.pxF
import kotlin.math.min

class BoxCircleCropTransformation(private val color: Int) : Transformation {

    override fun key(): String = BoxCircleCropTransformation::class.java.name + color.toString()

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val boxRoundBoundPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3.pxF
            setShadowLayer(1.pxF, 0f, 0f, 0xAA000000.toInt())
            color = this@BoxCircleCropTransformation.color
        }

        val minSize = min(input.width, input.height)
        val radius = minSize / 2f
        val output = pool.get(minSize, minSize, input.config)
        output.applyCanvas {
            drawCircle(radius, radius, radius, paint)
            paint.xfermode =
                XFERMODE
            drawBitmap(input, radius - input.width / 2f, radius - input.height / 2f, paint)
            drawCircle(radius, radius, radius - boxRoundBoundPaint.strokeWidth / 2f, boxRoundBoundPaint)
        }

        return output
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoxCircleCropTransformation

        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        return color
    }

    override fun toString(): String {
        return "BoxCircleCropTransformation(color=$color)"
    }

    private companion object {
        val XFERMODE = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
}
