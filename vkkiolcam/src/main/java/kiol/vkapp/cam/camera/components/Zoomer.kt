package kiol.vkapp.cam.camera.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.view.ScaleGestureDetector
import androidx.core.graphics.toRect
import kiol.vkapp.cam.camera.CameraConfigurator
import kotlin.math.max
import kotlin.math.min

class Zoomer(private val context: Context, private val backgroundHandler: Handler) {

    companion object {
        const val MaxZoom = 0.75f
    }

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var captureSession: CameraCaptureSession? = null

    private var cameraConfig: CameraConfigurator.Config? = null

    private var zLevel = 0.0f

    val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector
    .SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            val scaleFactor = detector?.scaleFactor
            scaleFactor?.let {
                zLevel -= (1.0f - scaleFactor)
                zLevel = max(min(zLevel, MaxZoom), 0f)
                setZoom(zLevel)
            }
            return true
        }
    })

    fun setConfg(cameraConfig: CameraConfigurator.Config) {
        this.cameraConfig = cameraConfig
    }

    fun setup(previewRequestBuilder: CaptureRequest.Builder, captureSession: CameraCaptureSession) {
        this.previewRequestBuilder = previewRequestBuilder
        this.captureSession = captureSession
    }

    fun setZoom(level: Float) {
        val li = max(min(level, MaxZoom), 0f)
        cameraConfig?.let {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(it.cameraId)

            val rect = RectF(characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE))
            val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            rect.let {
                val m = Matrix()
                m.preScale(1f - li, 1f - li, rect.width() / 2f, rect.height() / 2f)
                m.mapRect(rect)
                val zoom = rect.toRect()

                previewRequestBuilder?.let { builder ->
                    builder.set(CaptureRequest.SCALER_CROP_REGION, zoom)
                    val previewRequest = builder.build()
                    captureSession?.setRepeatingRequest(previewRequest, null, backgroundHandler)
                }
            }
        }
    }
}