package kiol.vkapp.taskc

import android.content.Context
import android.graphics.*
import android.media.ThumbnailUtils
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.toRectF
import androidx.core.graphics.withScale
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commonui.pxF
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

class MarkerImageGenerator(private val context: Context) {

    private val executorService = Executors.newSingleThreadExecutor()

    private val metrics = DisplayMetrics()
    private val badgeCache = HashMap<String, Bitmap>()

    private lateinit var photoStubBitmap: Bitmap
    private val photoStubWithBadgeCache = HashMap<String, Bitmap>()

    private var photoStubBitmapDescriptor: BitmapDescriptor? = null

    private inner class PlacePhotoBitmapTransformation : BitmapTransformation() {
        private val ID = "KIOL.PlacePhotoBitmapTransformation2"

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

        private fun getCroppedPhotoBitmap(bitmap: Bitmap): Bitmap {
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
    }

    private inner class PlaceBitmapTransformation : BitmapTransformation() {
        private val ID = "KIOL.PlaceBitmapTransformation2"

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

        private fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
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
    }


    private val textPaint = Paint().apply {
        textSize = 15.pxF
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val placeRoundBoundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10f
        setShadowLayer(3f, 0f, 0f, 0xAA000000.toInt())
        color = 0xFFFFFFFF.toInt()
    }

    private val badgePaint = Paint().apply {
        isAntiAlias = true
        color = 0xFF3F8AE0.toInt()
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

    init {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        precache()
    }

    private fun precache() {

        executorService.execute {
            val t = System.currentTimeMillis()
            createPhotoStubBitmap()

            repeat(25) {
                getBadge(it)
                createPhotoStubBitmapWithBadge(it)
            }

            val time = System.currentTimeMillis() - t

            Timber.d("Precache time: $time")
        }
    }

    fun getPhotoStubBimapDescriptor(): BitmapDescriptor {
        val b = photoStubBitmap
        if(photoStubBitmapDescriptor == null) {
            photoStubBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(b)
        }
        return photoStubBitmapDescriptor!!
    }

    fun getPhotoStubWithBadgeBimapDescriptor(count: Int): BitmapDescriptor {
        val text = getCountText(count)

        val b = photoStubWithBadgeCache[text]
        return BitmapDescriptorFactory.fromBitmap(b)
    }

    private fun createPhotoStubBitmap() {
        val outRect = Rect(0, 0, 150, 150)

        val output = Bitmap.createBitmap(
            150,
            150, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        outRect.inset(10 + 3, 10 + 3)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(outRect.toRectF(), 15f, 15f, placeRoundBoundPaint)

        photoStubBitmap = output
    }

    private fun createPhotoStubBitmapWithBadge(count: Int) {
        val text = getCountText(count)

        val nb = getBitmapWithBadge(photoStubBitmap, count)
        photoStubWithBadgeCache[text] = nb
    }

    private fun getBitmapWithBadge(bitmap: Bitmap, count: Int): Bitmap {
        val badgeBitmap = getBadge(count)

        val nb = Bitmap.createBitmap(
            bitmap.width + badgeBitmap.width / 5,
            bitmap.height + badgeBitmap.height / 5,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(nb)
        canvas.drawBitmap(bitmap, 0f, badgeBitmap.height / 5f, null)
        canvas.drawBitmap(badgeBitmap, (nb.width - badgeBitmap.width).toFloat(), 0f, null)

        return nb
    }

    private fun getBadge(count: Int): Bitmap {
        val text = getCountText(count)

        val cachedBadge = badgeCache[text]
        if (cachedBadge != null) {
            return cachedBadge
        }

        val widthF = textPaint.measureText(text)
        val textMetrics = textPaint.fontMetrics
        val heightF = Math.abs(textMetrics.bottom) + Math.abs(textMetrics.top)

        val width = 1.2f * max(widthF, heightF)
        val height = 1.2f * heightF

        val bitmap = Bitmap.createBitmap(width.roundToInt(), height.roundToInt(), Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)

        canvas.drawRoundRect(RectF(0f, 0f, width, height), height, height, badgePaint)
        canvas.drawText(text, width / 2f, height - (height - heightF / 2f) / 2f, textPaint)

        badgeCache[text] = bitmap

        return bitmap
    }

    private fun getCountText(count: Int) = if (count > 19) "19+" else count.toString()

    private fun getTransformation(place: Place): BitmapTransformation {
        return when (place.placeType) {
            PlaceType.Photos -> PlacePhotoBitmapTransformation()
            else -> PlaceBitmapTransformation()
        }
    }

    fun loadPlacemarkImageWithCount(context: Context, place: Place, marker: Marker, count: Int) {
        val transform = getTransformation(place)

        Glide.with(context).asBitmap().load(place.photo).transform(transform).into(object :
            SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val withBadge = getBitmapWithBadge(resource, count)
                if (marker.tag != null) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(withBadge))
                }
            }
        })
    }

    fun loadPlacemarkImage(context: Context, place: Place, marker: Marker) {
        val transform = getTransformation(place)

        Glide.with(context).asBitmap().load(place.photo).transform(transform).into(object :
            SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (marker.tag != null) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource))
                }
            }
        })
    }
}