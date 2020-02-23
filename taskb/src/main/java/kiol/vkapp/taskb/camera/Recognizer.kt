package kiol.vkapp.taskb.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.media.ImageReader
import android.os.Handler
import android.view.TextureView
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.TheApp
import ru.timepad.domain.qr.QRBarRecognizer
import timber.log.Timber
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

class Vector(x1: Float = 0f, y1: Float = 0f, x2: Float = 0f, y2: Float = 0f) {

    var vec = PointF()
    var len = 0f

    init {
        set(x1, y1, x2, y2)
    }

    fun set(x1: Float = 0f, y1: Float = 0f, x2: Float = 0f, y2: Float = 0f) {
        vec.x = x2 - x1
        vec.y = y2 - y1

        len = sqrt(vec.x.pow(2) + vec.y.pow(2))
    }
}

fun scalar(v1: Vector, v2: Vector): Float {
    val t = v1.vec.x * v2.vec.x + v1.vec.y * v2.vec.y
    val g = v1.len * v2.len
    return t / g
}

class Recognizer(context: Context, private val backgroundHandler: Handler) {

    private val compositeDisposable = CompositeDisposable()

    private lateinit var qrOverlay: QrOverlay

    private val qrBarRecognizer = (context.applicationContext as TheApp).qrBarRecognizer
    val imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2).apply {
        setOnImageAvailableListener({ reader ->
            val img = reader.acquireLatestImage()
            img?.let {
                val buf = img.planes[0].buffer
                val data = ByteArray(buf.remaining())
                buf.get(data)
                val w = img.width
                val h = img.height
                img.close()

                qrBarRecognizer.recognize(QRBarRecognizer.ImageData(data, w, h))
            }
        }, backgroundHandler)
    }

    private val floatArrayPoints = FloatArray(8)
    private val floatArrayFinishPoints = FloatArray(2)
    private val m1 = Matrix()
    private val m2 = Matrix()
    private val tempRect1 = RectF()
    private val tempRect2 = RectF()

    private val tempVector1 = Vector()
    private val tempVector2 = Vector()

    fun setViews(qrOverlay: QrOverlay) {
        this.qrOverlay = qrOverlay

        compositeDisposable.add(qrBarRecognizer.subscribe().map {
            parse(it)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            qrOverlay.drawQr(it)
        }, {
            Timber.e("Recognize error")
        }))
    }

    fun release() {
        compositeDisposable.clear()
    }

    private fun parse(result: QRBarRecognizer.Result): QrDrawModel {
        val points = result.points
        if (points.size == 4) {
            floatArrayPoints[0] = points[0].x
            floatArrayPoints[1] = points[0].y

            floatArrayPoints[2] = points[1].x
            floatArrayPoints[3] = points[1].y

            floatArrayPoints[4] = points[2].x
            floatArrayPoints[5] = points[2].y

            floatArrayPoints[6] = points[3].x
            floatArrayPoints[7] = points[3].y

            m1.reset()
            m1.preRotate(90f)
            m1.preTranslate(0f, -480f)

            m1.mapPoints(floatArrayPoints)


            val xa = floatArrayPoints.filterIndexed { index, fl -> index % 2 == 0 }.sum() / 4f

            val ya = floatArrayPoints.filterIndexed { index, fl -> index % 2 != 0 }.sum() / 4f

            val r = sqrt((xa - floatArrayPoints[0]).pow(2) + (ya - floatArrayPoints[1]).pow(2))

            tempVector1.set(xa, ya, xa + r, ya - r)
            tempVector2.set(xa, ya, floatArrayPoints[0], floatArrayPoints[1])

            val rv = tempVector1
            val lv = tempVector2

            val v = acos(scalar(rv, lv))
            val z = rv.vec.x * lv.vec.y - rv.vec.y * lv.vec.x

            val angle = v * sign(z) * 180 / Math.PI.toFloat()

            tempRect1.set(0f, 0f, 480f, 640f)
            tempRect2.set(0f, 0f, qrOverlay.width.toFloat(), qrOverlay.height.toFloat())
            m2.reset()
            m2.setRectToRect(tempRect1, tempRect2, Matrix.ScaleToFit.CENTER)

            floatArrayFinishPoints[0] = xa
            floatArrayFinishPoints[1] = ya
            m2.mapPoints(floatArrayFinishPoints)
            val r2 = m2.mapRadius(r)

            return QrDrawModel(floatArrayFinishPoints[0], floatArrayFinishPoints[1], r2, angle)
        }

        return QrOverlay.EmptyQrDrawModel
    }
}