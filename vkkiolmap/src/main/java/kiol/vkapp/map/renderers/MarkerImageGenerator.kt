package kiol.vkapp.map.renderers

import android.content.Context
import android.graphics.*
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Handler
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRectF
import coil.ImageLoader
import coil.request.ImageRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.Cluster
import kiol.vkapp.commondata.domain.BoxType.*
import kiol.vkapp.commondata.domain.Place
import kiol.vkapp.commondata.domain.PlaceType
import kiol.vkapp.commonui.px
import kiol.vkapp.commonui.pxF
import kiol.vkapp.map.clusters.PlaceClusterItem
import kiol.vkapp.map.renderers.transformations.BoxCircleCropTransformation
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

    internal data class BdKey(val path: String, val color: Int)

    companion object {

        private val PLACE_SIZE = 40.px

        private val PHOTO_SIZE = 60.px
        private val PHOTO_RADIUS = 6.pxF
        private val BOUND_STOKER_WIDTH = 3.px
        private val BOUND_SHADOW_RADIUS = 1.px
    }

    private val imageLoader = ImageLoader(context)
    private val markerCache = hashMapOf<BdKey, BitmapDescriptor>()

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
    private val photoBitmapTransformation = PlacePhotoBitmapTransformation()

    private val rndColors = listOf(0xFF71AAEB.toInt())

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
        private val ID = "KIOL.PlaceBitmapTransformation2$color"

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
            createPlaceStubBitmap()

            repeat(25) {
                createPlaceWithCountBitmap(it)
            }
        }
    }

    private fun getPhotoStubBimapDescriptor(): BitmapDescriptor {
        val b = photoStubBitmap
        if (photoStubBitmapDescriptor == null) {
            Timber.w("kiol BitmapDescriptorFactory called")
            photoStubBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(b)
        }
        return photoStubBitmapDescriptor!!
    }

    private fun getPlaceStubBimapDescriptor(): BitmapDescriptor {
        val b = placeStubBitmap
        if (placeStubBitmapDescriptor == null) {
            Timber.w("kiol BitmapDescriptorFactory called")
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
                Timber.w("kiol BitmapDescriptorFactory called")
                d = BitmapDescriptorFactory.fromBitmap(b)
                photoStubDescriptorWithBadgeCache[text] = d
            }

            d!!
        } else {
            val b = placeStubWithBadgeCache[text]
            var d = placeStubDescriptorWithBadgeCache[text]
            if (d == null) {
                Timber.w("kiol BitmapDescriptorFactory called")
                d = BitmapDescriptorFactory.fromBitmap(b)
                placeStubDescriptorWithBadgeCache[text] = d
            }
            d!!
        }
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
            else -> placeBitmapTransformation
        }
    }

    fun loadPhotoClusterImageWithCount(context: Context, place: Place, marker: Marker, count: Int) {
        val transform = getTransformation(place)

        val photoPath = if (place.placeType == PlaceType.Box) Uri.parse(place.photo) else place.photo
        Glide.with(context).asBitmap().load(photoPath).transform(transform).into(object :
            SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (marker.tag != null) {
                    executorService.execute {
                        val withBadge = getPhotoBitmapWithBadge(resource, count)
                        uiHandler.post {
                            if (marker.tag != null) {
                                Timber.w("kiol BitmapDescriptorFactory called")
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(withBadge))
                            }
                        }
                    }
                }
            }
        })
    }

    fun loadPlacemarkImage(context: Context, place: Place, marker: Marker) {
        Timber.w("kiol loadPlacemarkImage called")
        val color = when (place.customPlaceParams?.box?.boxType) {
            Ok -> 0xFF4BB34B.toInt()
            Fraud -> 0xFFFF5C5C.toInt()
            Unknown -> 0xFF76787A.toInt()
            else -> 0xFF76787A.toInt()
        }
        val key = BdKey(place.photo, color)

        var descriptor = markerCache[key]
        if (descriptor == null) {
            val request = ImageRequest.Builder(context)
                .data(place.photo)
                .size(40.px, 40.px)
                .allowRgb565(true)
                .transformations(BoxCircleCropTransformation(color))
                .target { drawable ->
                    if (marker.tag != null) {
                        Timber.w("kiol BitmapDescriptorFactory called")
                        descriptor = BitmapDescriptorFactory.fromBitmap(drawable.toBitmap())
                        markerCache[key] = descriptor!!
                        marker.setIcon(descriptor)
                    }
                }
                .build()
            val disposable = imageLoader.enqueue(request)
        } else {
            if (marker.tag != null) {
                marker.setIcon(descriptor)
            }
        }
    }
}