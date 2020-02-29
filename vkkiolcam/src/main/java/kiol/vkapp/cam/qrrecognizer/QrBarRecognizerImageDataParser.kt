package ru.timepad.domain.qr

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import timber.log.Timber

class QrBarRecognizerImageDataParser {

    private val allReader = MultiFormatReader()
    private val hints = mapOf(
        Pair(
            DecodeHintType.POSSIBLE_FORMATS, listOf(
                BarcodeFormat.QR_CODE
            )
        )
    )

    fun recognize(img: QRBarRecognizer.ImageData): QRBarRecognizer.Result? {
        return try {
            val source = PlanarYUVLuminanceSource(
                img.buffer,
                img.width,
                img.height,
                0,
                0,
                img.width,
                img.height,
                false
            )
            val bm = BinaryBitmap(HybridBinarizer(source))
            val result = allReader.decode(bm, hints)
            val type = if (result.barcodeFormat == BarcodeFormat.QR_CODE)
                QRBarRecognizer.Type.QR
            else
                QRBarRecognizer.Type.BAR

            QRBarRecognizer.Result(result.text, type, result.resultPoints.toList())
        } catch (e: Exception) {
            Timber.d("zxing exception: $e")
            null
        }

    }
}