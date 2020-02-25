package kiol.vkapp.taskc

import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.toRectF
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import kiol.vkapp.commondata.domain.Place
import java.util.*


fun loadPlacemarkImage(context: Context, place: Place, placemarkMapObject: PlacemarkMapObject) {
    if (place is Place.GroupPlace) {
        Glide.with(context).asBitmap().load(place.groupPhoto).into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                placemarkMapObject.setIcon(object : ImageProvider() {
                    override fun getId(): String {
                        return "bitmap:" + UUID.randomUUID().toString()
                    }

                    override fun getImage(): Bitmap {
                        return getCroppedBitmap(resource)
                    }

                }, IconStyle())
            }
        })
    }
}


private val placeRoundPaint = Paint().apply {
    isAntiAlias = true
    color = -0xbdbdbe
}

private val placeCropPaint = Paint().apply {
    isAntiAlias = true
    color = -0xbdbdbe
    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
}

private val placeRoundBoundPaint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeWidth = 10f
    color = 0xFFFFFFFF.toInt()
}

fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
    val outRect = Rect(0, 0, 100, 100)

    val output = Bitmap.createBitmap(
        100,
        100, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    canvas.drawARGB(0, 0, 0, 0)
    // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
    canvas.drawCircle(
        output.width / 2f, output.height / 2f,
        output.width / 2f, placeRoundPaint
    )
    canvas.drawBitmap(bitmap, rect, outRect, placeCropPaint)
    outRect.inset(5, 5)
    canvas.drawArc(outRect.toRectF(), 0f, 360f, false, placeRoundBoundPaint)
    //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
    //return _bmp;
    return output
}

class TextImageProvider(private val context: Context, private val text: String) : ImageProvider() {
    override fun getId(): String {
        return "text_$text"
    }

    override fun getImage(): Bitmap {
        val metrics = DisplayMetrics()
        val manager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.defaultDisplay.getMetrics(metrics)
        val textPaint = Paint()
        textPaint.textSize = 15 * metrics.density
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true
        val widthF = textPaint.measureText(text)
        val textMetrics = textPaint.fontMetrics
        val heightF = Math.abs(textMetrics.bottom) + Math.abs(textMetrics.top)
        val textRadius = Math.sqrt(widthF * widthF + heightF * heightF.toDouble()).toFloat() / 2
        val internalRadius: Float = textRadius + 3 * metrics.density
        val externalRadius: Float = internalRadius + 3 * metrics.density
        val width = (2 * externalRadius + 0.5).toInt()
        val bitmap =
            Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint()
        backgroundPaint.isAntiAlias = true
        backgroundPaint.color = Color.WHITE
        canvas.drawCircle(width / 2.toFloat(), width / 2.toFloat(), externalRadius, backgroundPaint)
        backgroundPaint.color = Color.RED
        canvas.drawCircle(width / 2.toFloat(), width / 2.toFloat(), internalRadius, backgroundPaint)
        canvas.drawText(
            text,
            width / 2.toFloat(),
            width / 2 - (textMetrics.ascent + textMetrics.descent) / 2,
            textPaint
        )
        return bitmap
    }
}
