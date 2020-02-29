package kiol.vkapp.cam.camera.components

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.os.Handler

class Torch(private val backgroundHandler: Handler) {

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var captureSession: CameraCaptureSession? = null

    var isTorchEnabled = false

    fun setup(previewRequestBuilder: CaptureRequest.Builder, captureSession: CameraCaptureSession) {
        this.previewRequestBuilder = previewRequestBuilder
        this.captureSession = captureSession
    }

    fun setEnabled(value: Boolean) {
        if (isTorchEnabled != value) {
            previewRequestBuilder?.let { builder ->
                builder.set(
                    CaptureRequest.FLASH_MODE,
                    if (value) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                )
                val previewRequest = builder.build()
                captureSession?.setRepeatingRequest(previewRequest, null, backgroundHandler)
                isTorchEnabled = value
            }
        }
    }

    fun reset() {
        isTorchEnabled = false
        previewRequestBuilder = null
        captureSession = null
    }
}