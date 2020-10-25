package kiol.vkapp.map.renderers

import android.content.Context
import android.graphics.*
import android.media.ThumbnailUtils
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.toRectF
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.Cluster
import kiol.vkapp.commondata.domain.BoxType
import kiol.vkapp.commondata.domain.BoxType.*
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commonui.px
import kiol.vkapp.commonui.pxF
import kiol.vkapp.map.PlaceClusterItem
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

fun Cluster<PlaceClusterItem>.getPlaceType(): PlaceType {
    return items.firstOrNull()?.place?.placeType ?: PlaceType.Groups
}

fun Cluster<PlaceClusterItem>.getPlace(): Place? {
    return items.firstOrNull()?.place
}

class MarkerImageGenerator(private val context: Context) {

    companion object {

        private val PLACE_SIZE = 40.px

        private val PHOTO_SIZE = 60.px
        private val PHOTO_RADIUS = 6.pxF
        private val BOUND_STOKER_WIDTH = 3.px
        private val BOUND_SHADOW_RADIUS = 1.px
    }

    private val uiHandler = Handler()
    private val executorService = Executors.newFixedThreadPool(3)

    private val metrics = DisplayMetrics()
    private val photoBadgeCache = HashMap<String, Bitmap>()

    private lateinit var photoStubBitmap: Bitmap
    private lateinit var placeStubBitmap: Bitmap
    private val photoStubWithBadgeCache = HashMap<String, Bitmap>()
    private val placeStubWithBadgeCache = HashMap<String, Bitmap>()
    private val photoStubDescriptorWithBadgeCache = HashMap<String, BitmapDescriptor>()
    private val placeStubDescriptorWithBadgeCache = HashMap<String, BitmapDescriptor>()

    private var photoStubBitmapDescriptor: BitmapDescriptor? = null
    private var placeStubBitmapDescriptor: BitmapDescriptor? = null

    private val placeBitmapTransformation = PlaceBitmapTransformation()
    private val boxBitmapTransformationMap = mapOf(
        Color.GREEN to PlaceBitmapTransformation(Color.GREEN),
        Color.RED to PlaceBitmapTransformation(Color.RED),
        Color.GRAY to PlaceBitmapTransformation(Color.GRAY)
    )
    private val photoBitmapTransformation = PlacePhotoBitmapTransformation()

    private val rndColors = listOf(Color.BLUE, Color.RED, Color.GRAY, Color.MAGENTA, Color.BLACK)

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
            val outRect = Rect(
                0, 0,
                PHOTO_SIZE,
                PHOTO_SIZE
            )

            val output = Bitmap.createBitmap(
                PHOTO_SIZE,
                PHOTO_SIZE, Bitmap.Config.ARGB_8888
            )
            val square = ThumbnailUtils.extractThumbnail(
                bitmap,
                PHOTO_SIZE,
                PHOTO_SIZE
            )
            val canvas = Canvas(output)

            outRect.inset(BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS, BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS)

            val rect = Rect(0, 0, square.width, square.height)
            canvas.drawARGB(0, 0, 0, 0)

            canvas.drawRoundRect(
                outRect.toRectF(),
                PHOTO_RADIUS,
                PHOTO_RADIUS, placeRoundPaint
            )
            canvas.drawBitmap(square, rect, outRect, placeCropPaint)
            canvas.drawRoundRect(
                outRect.toRectF(),
                PHOTO_RADIUS,
                PHOTO_RADIUS, placeRoundBoundPaint
            )

            return output
        }
    }

    private inner class PlaceBitmapTransformation(private val color: Int? = null) : BitmapTransformation() {
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
            val outRect = Rect(
                0, 0,
                PLACE_SIZE,
                PLACE_SIZE
            )
            outRect.inset(BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS, BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS)

            val output = Bitmap.createBitmap(
                PLACE_SIZE,
                PLACE_SIZE, Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(output)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(
                output.width / 2f, output.height / 2f,
                PLACE_SIZE / 2f - (BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS), placeRoundPaint
            )
            canvas.drawBitmap(bitmap, rect, outRect, placeCropPaint)

            if (color != null) {
                boxRoundBoundPaint.color = color
                canvas.drawArc(outRect.toRectF(), 0f, 360f, false, boxRoundBoundPaint)
            } else {
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
        strokeWidth = BOUND_STOKER_WIDTH.toFloat()
        setShadowLayer(BOUND_SHADOW_RADIUS.toFloat(), 0f, 0f, 0xAA000000.toInt())
        color = 0xFFFFFFFF.toInt()
    }

    private val boxRoundBoundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = BOUND_STOKER_WIDTH.toFloat()
        setShadowLayer(BOUND_SHADOW_RADIUS.toFloat(), 0f, 0f, 0xAA000000.toInt())
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

    private val placeCirclePaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFF0000.toInt()
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
            createPlaceStubBitmap()

            repeat(25) {
                getPhotoBadge(it)
                createPhotoStubBitmapWithBadge(it)
                createPlaceWithCountBitmap(it)
            }

            val time = System.currentTimeMillis() - t

            Timber.d("Precache time: $time")
        }
    }

    private fun getPhotoStubBimapDescriptor(): BitmapDescriptor {
        val b = photoStubBitmap
        if (photoStubBitmapDescriptor == null) {
            photoStubBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(b)
        }
        return photoStubBitmapDescriptor!!
    }

    private fun getPlaceStubBimapDescriptor(): BitmapDescriptor {
        val b = placeStubBitmap
        if (placeStubBitmapDescriptor == null) {
            placeStubBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(b)
        }
        return placeStubBitmapDescriptor!!
    }

    fun getStubBimapDescriptor(place: Place): BitmapDescriptor {
        return if (place.placeType == PlaceType.Photos) {
            getPhotoStubBimapDescriptor()
        } else {
            getPlaceStubBimapDescriptor()
        }
    }

    fun getClusterBitmapDescriptor(cluster: Cluster<PlaceClusterItem>): BitmapDescriptor {
        val text = getCountText(cluster.size)

        return if (cluster.getPlaceType() == PlaceType.Photos) {
            val b = photoStubWithBadgeCache[text]

            var d = photoStubDescriptorWithBadgeCache[text]
            if (d == null) {
                d = BitmapDescriptorFactory.fromBitmap(b)
                photoStubDescriptorWithBadgeCache[text] = d
            }

            d!!
        } else {
            val b = placeStubWithBadgeCache[text]
            var d = placeStubDescriptorWithBadgeCache[text]
            if (d == null) {
                d = BitmapDescriptorFactory.fromBitmap(b)
                placeStubDescriptorWithBadgeCache[text] = d
            }
            d!!
        }
    }

    private fun createPhotoStubBitmap() {
        val outRect = Rect(
            0, 0,
            PHOTO_SIZE,
            PHOTO_SIZE
        )

        val output = Bitmap.createBitmap(
            PHOTO_SIZE,
            PHOTO_SIZE, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        outRect.inset(BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS, BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(
            outRect.toRectF(),
            PHOTO_RADIUS,
            PHOTO_RADIUS, placeRoundBoundPaint
        )

        photoStubBitmap = output
    }

    private fun createPlaceStubBitmap() {
        val outRect = Rect(
            0, 0,
            PLACE_SIZE,
            PLACE_SIZE
        )

        val output = Bitmap.createBitmap(
            PLACE_SIZE,
            PLACE_SIZE, Bitmap.Config.ARGB_8888
        )

        outRect.inset(BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS, BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS)

        val canvas = Canvas(output)

        placeCirclePaint.color = rndColors.random()
        canvas.drawCircle(
            output.width / 2f, output.height / 2f,
            PLACE_SIZE / 2f - (BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS), placeCirclePaint
        )
        canvas.drawArc(outRect.toRectF(), 0f, 360f, false, placeRoundBoundPaint)

        placeStubBitmap = output
    }

    private fun createPlaceWithCountBitmap(count: Int) {
        val text = getCountText(count)

        val widthF = textPaint.measureText(text)
        val textMetrics = textPaint.fontMetrics
        val heightF = Math.abs(textMetrics.bottom) + Math.abs(textMetrics.top)

        val width = 1.8f * max(widthF, heightF)
        val height = 1.8f * heightF

        val maxSize = max(width, height)

        val bitmap = Bitmap.createBitmap(
            maxSize.roundToInt(),
            maxSize.roundToInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        rect.inset(BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS, BOUND_STOKER_WIDTH + BOUND_SHADOW_RADIUS)
        val radius = max(rect.width().toFloat(), rect.height().toFloat()) - BOUND_SHADOW_RADIUS

        placeCirclePaint.color = rndColors.random()
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, radius / 2f, placeCirclePaint)
        canvas.drawText(text, bitmap.width / 2f, bitmap.height - (bitmap.height - heightF / 2f) / 2f, textPaint)
        canvas.drawArc(
            rect.toRectF(),
            0f,
            360f,
            false,
            placeRoundBoundPaint
        )

        placeStubWithBadgeCache[text] = bitmap
    }

    private fun createPhotoStubBitmapWithBadge(count: Int) {
        val text = getCountText(count)

        val nb = getPhotoBitmapWithBadge(photoStubBitmap, count)
        photoStubWithBadgeCache[text] = nb
    }

    private fun getPhotoBitmapWithBadge(bitmap: Bitmap, count: Int): Bitmap {
        val badgeBitmap = getPhotoBadge(count)

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

    private fun getPhotoBadge(count: Int): Bitmap {
        val text = getCountText(count)

        val cachedBadge = photoBadgeCache[text]
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

        photoBadgeCache[text] = bitmap

        return bitmap
    }

    private fun getCountText(count: Int) = if (count > 19) "19+" else count.toString()

    private fun getTransformation(place: Place): BitmapTransformation {
        return when (place.placeType) {
            PlaceType.Photos -> photoBitmapTransformation
            else -> {
                if (place.customPlaceParams != null) {
                    val box = place.customPlaceParams?.box
                    val color = when (box?.boxType) {
                        Ok -> Color.GREEN
                        Fraud -> Color.RED
                        Unknown -> Color.GRAY
                        else -> Color.GRAY
                    }
                    boxBitmapTransformationMap[color] ?: placeBitmapTransformation
                } else {
                    placeBitmapTransformation
                }
            }
        }
    }

    fun loadPhotoClusterImageWithCount(context: Context, place: Place, marker: Marker, count: Int) {
        val transform = getTransformation(place)

        Glide.with(context).asBitmap().load(place.photo).transform(transform).into(object :
            SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (marker.tag != null) {
                    executorService.execute {
                        val withBadge = getPhotoBitmapWithBadge(resource, count)
                        uiHandler.post {
                            if (marker.tag != null) {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(withBadge))
                            }
                        }
                    }
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