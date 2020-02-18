package ru.timepad.domain.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import kiol.vkapp.taskb.qrrecognizer.MyPlanarYUVLuminanceSource
import timber.log.Timber

class QrBarRecognizerImageDataParser constructor() {

    private val allReader = MultiFormatReader()
    private val hints = mapOf(
        Pair(
            DecodeHintType.POSSIBLE_FORMATS, listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.CODABAR,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED
            )
        )
    )

    fun recognize(img: QRBarRecognizer.ImageData): QRBarRecognizer.Result? {
        return  try {
            val source = MyPlanarYUVLuminanceSource(img.buffer, img.width, img.height, 0, 0, img.width, img.height).rotateCounterClockwise()
            val bm = BinaryBitmap(HybridBinarizer(source))
            val result = allReader.decode(bm, hints)
            val type = if (result.barcodeFormat == BarcodeFormat.QR_CODE) QRBarRecognizer.Type.QR else QRBarRecognizer.Type.BAR
            QRBarRecognizer.Result(result.text, type)
        } catch (e: Exception) {
            Timber.d("zxing exception: $e")
            null
        }

    }
}