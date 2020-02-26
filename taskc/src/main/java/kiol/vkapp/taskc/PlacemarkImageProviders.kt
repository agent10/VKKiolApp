package kiol.vkapp.taskc

import android.content.Context
import android.graphics.*
import android.media.ThumbnailUtils
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.BitmapCompat
import androidx.core.graphics.toRectF
import androidx.core.graphics.withScale
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commondata.domain.PlaceType.*
import kiol.vkapp.commonui.px
import kiol.vkapp.commonui.pxF
import timber.log.Timber
import java.security.MessageDigest
import java.util.*
import kotlin.math.roundToInt


fun loadPlacemarkImage(context: Context, place: Place, placemarkMapObject: PlacemarkMapObject) {
    placemarkMapObject.setIcon(object : ImageProvider() {
        override fun getId(): String {
            return "bitmap:" + UUID.randomUUID().toString()
        }

        override fun getImage(): Bitmap {

            Timber.d("loadPlacemarkImage, getImage. v = ${placemarkMapObject.isVisible}")
            return getStubBitmap()
        }

    }, IconStyle())

    //    Glide.with(context).asBitmap().load(place.photo).into(object : SimpleTarget<Bitmap>() {
    //        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
    //            placemarkMapObject.setIcon(object : ImageProvider() {
    //                override fun getId(): String {
    //                    return "bitmap:" + UUID.randomUUID().toString()
    //                }
    //
    //                override fun getImage(): Bitmap {
    //                    if (place.placeType == PlaceType.Photos) {
    //                        return getCroppedPhotoBitmap(resource)
    //                    } else {
    //                        return getCroppedBitmap(resource)
    //                    }
    //                }
    //
    //            }, IconStyle())
    //        }
    //    })
}

fun loadPlacemarkImage(context: Context, place: Place, marker: MarkerOptions) {
    val transform = when (place.placeType) {
        Photos -> PlacePhotoBitmapTransformation()
        else -> PlaceBitmapTransformation()
    }

    Glide.with(context).asBitmap().load(place.photo).transform(transform).into(object :
        SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            marker.icon(BitmapDescriptorFactory.fromBitmap(resource))
        }
    })
}

fun loadPlacemarkImageWithCount(context: Context, place: Place, marker: Marker, count: Int) {
    val transform = when (place.placeType) {
        Photos -> PlacePhotoBitmapTransformation()
        else -> PlaceBitmapTransformation()
    }

    Glide.with(context).asBitmap().load(place.photo).transform(transform).into(object :
        SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            val withBadge = getCroppedPhotoBitmapWithBadge(context, resource, count)
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(withBadge))
        }
    })
}

fun loadPlacemarkImage(context: Context, place: Place, marker: Marker) {
    val transform = when (place.placeType) {
        Photos -> PlacePhotoBitmapTransformation()
        else -> PlaceBitmapTransformation()
    }

    Glide.with(context).asBitmap().load(place.photo).transform(transform).into(object :
        SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource))
        }
    })
}

private class PlacePhotoBitmapTransformation : BitmapTransformation() {
    private val ID = "KIOL.PlacePhotoBitmapTransformation"

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray())
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return getCroppedPhotoBitmap(toTransform)
    }

    override fun equals(other: Any?): Boolean {
        return other is PlacePhotoBitmapTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }
}

private class PlaceBitmapTransformation : BitmapTransformation() {
    private val ID = "KIOL.PlaceBitmapTransformation"

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray())
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return getCroppedBitmap(toTransform)
    }

    override fun equals(other: Any?): Boolean {
        return other is PlaceBitmapTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode()
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
    setShadowLayer(3f, 0f, 0f, 0xAA000000.toInt())
    color = 0xFFFFFFFF.toInt()
}

private val placeRoundStubPaint = Paint().apply {
    isAntiAlias = true
    color = 0xFFFFFF00.toInt()
}

fun getStubBitmap(): Bitmap {
    val outRect = Rect(0, 0, 100, 100)
    val output = Bitmap.createBitmap(
        100,
        100, Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)
    canvas.drawCircle(
        output.width / 2f, output.height / 2f,
        output.width / 2f, placeRoundStubPaint
    )
    outRect.inset(5, 5)
    canvas.drawArc(outRect.toRectF(), 0f, 360f, false, placeRoundBoundPaint)
    return output
}

fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
    val outRect = Rect(0, 0, 100, 100)

    val output = Bitmap.createBitmap(
        100,
        100, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    canvas.withScale(0.95f, 0.95f, 50f, 50f) {
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
    }

    return output
}

fun getCroppedPhotoStubBitmap(): Bitmap {
    val outRect = Rect(0, 0, 150, 150)

    val output = Bitmap.createBitmap(
        150,
        150, Bitmap.Config.ARGB_8888
    )
    //val square = ThumbnailUtils.extractThumbnail(bitmap, 150, 150)
    val canvas = Canvas(output)

    outRect.inset(10 + 3, 10 + 3)

    //val rect = Rect(0, 0, square.width, square.height)
    canvas.drawARGB(0, 0, 0, 0)

    //    canvas.drawRoundRect(outRect.toRectF(), 15f, 15f, placeRoundPaint)
    //canvas.drawBitmap(square, rect, outRect, placeCropPaint)
    canvas.drawRoundRect(outRect.toRectF(), 15f, 15f, placeRoundBoundPaint)

    return output
}

fun getCroppedPhotoBitmap(bitmap: Bitmap): Bitmap {
    val outRect = Rect(0, 0, 150, 150)

    val output = Bitmap.createBitmap(
        150,
        150, Bitmap.Config.ARGB_8888
    )
    val square = ThumbnailUtils.extractThumbnail(bitmap, 150, 150)
    val canvas = Canvas(output)

    outRect.inset(10 + 3, 10 + 3)

    val rect = Rect(0, 0, square.width, square.height)
    canvas.drawARGB(0, 0, 0, 0)

    canvas.drawRoundRect(outRect.toRectF(), 15f, 15f, placeRoundPaint)
    canvas.drawBitmap(square, rect, outRect, placeCropPaint)
    canvas.drawRoundRect(outRect.toRectF(), 15f, 15f, placeRoundBoundPaint)

    return output
}

fun getCroppedPhotoBitmapWithBadge(context: Context, bitmap: Bitmap, count: Int): Bitmap {
    val nb = Bitmap.createBitmap(bitmap)
    val canvas = Canvas(nb)
    val badgeBitmap = getBadge(context, count)
    canvas.drawBitmap(badgeBitmap, (nb.width / 2f).toFloat(), 20f, null)
    return nb
}

private fun getBadge(context: Context, count: Int): Bitmap {
    val text = if (count > 10) "10+" else count.toString()

    val metrics = DisplayMetrics()
    val manager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    manager.defaultDisplay.getMetrics(metrics)
    val textPaint = Paint()
    textPaint.textSize = 15.pxF
    textPaint.color = Color.WHITE
    textPaint.textAlign = Paint.Align.CENTER
    textPaint.style = Paint.Style.FILL
    textPaint.isAntiAlias = true
    val widthF = textPaint.measureText(text)
    val textMetrics = textPaint.fontMetrics
    val heightF = Math.abs(textMetrics.bottom) + Math.abs(textMetrics.top)

    val textRadius = Math.sqrt(widthF * widthF + heightF * heightF.toDouble()).toFloat() / 2
    val bitmap = Bitmap.createBitmap(widthF.roundToInt(), heightF.roundToInt(), Bitmap.Config.ARGB_8888)

    val internalRadius: Float = textRadius + 3 * metrics.density
    val externalRadius: Float = internalRadius + 3 * metrics.density
    val width = (2 * externalRadius + 0.5).toInt()

    val canvas = Canvas(bitmap)
    val backgroundPaint = Paint()
    backgroundPaint.isAntiAlias = true
    backgroundPaint.color = Color.BLUE

    canvas.drawRoundRect(RectF(0f,0f,widthF,heightF), 10f, 10f, backgroundPaint)
    canvas.drawText(
        text,
        widthF / 2,
        widthF / 2f - (textMetrics.ascent + textMetrics.descent) / 2,
        textPaint
    )

    return bitmap
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
        textPaint.textSize = 15.pxF
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
