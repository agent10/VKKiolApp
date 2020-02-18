package ru.timepad.domain.qr

import android.media.Image
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.nio.ByteBuffer

class QRBarRecognizer constructor(
    private val qrBarRecognizerImageDataParser: QrBarRecognizerImageDataParser,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private companion object {
        const val MinTimeout = 200L
    }

    enum class Type {
        QR, BAR
    }

    class ImageData(val buffer: ByteArray, val width: Int, val height: Int)

    data class Result(val text: String, val type: Type, val points: List<ResultPoint>)

    @Volatile
    private var lastResult: Result? = null

    @Volatile
    private var lastTimestamp = 0L

    private val resultStream = PublishProcessor.create<Result>()

    @Volatile
    var enabled = true
        set(value) {
            field = value
            Timber.d("QRBarRecognizer enabled: $value")
        }

    private val stream = resultStream.filter {
        it != lastResult || clock() - lastTimestamp > MinTimeout
    }.doOnNext {
        lastResult = it
        lastTimestamp = clock()
    }


    fun recognize(img: ImageData) {
        if (enabled) {
            Completable.fromAction {
                val result = qrBarRecognizerImageDataParser.recognize(img)
                if (result == null) {
                    Timber.d("zxing null result")
                }
                result?.let {
                    resultStream.onNext(it)
                }
            }.doOnError { Timber.e("Image recognize failed: $it") }.onErrorComplete().subscribeOn(Schedulers.computation())
                .subscribe()
        }
    }

    fun subscribe(): Flowable<Result> = stream
}