package kiol.vkapp.taskb.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Size
import kiol.vkapp.taskb.CompareSizesByArea
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class CameraConfigurator(private val context: Context) {
    class CameraNotFoundException : Exception("Camera not found")

    class Config(val cameraId: String, val previewSize: Size, val mediaRecorderSize: Array<Size>)

    fun createConfig(displaySize: Point? = null, width: Int, height: Int, isFaceCamera: Boolean): Config {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)

            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection == CameraCharacteristics.LENS_FACING_FRONT && isFaceCamera ||
                cameraDirection == CameraCharacteristics.LENS_FACING_BACK && !isFaceCamera
            ) {

                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue

                val swappedDimensions = width < height

                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height

                val previewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    rotatedPreviewWidth, rotatedPreviewHeight
                )

                val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                Timber.d("hardwareLevel: $hardwareLevel")

                val yuvAvailableSizes = map.getOutputSizes(ImageFormat.YUV_420_888)
                Timber.d("YUV available sizes: ${yuvAvailableSizes.toList()}")

                val mediaRecorderSizes = map.getOutputSizes(MediaRecorder::class.java)
                Timber.d("MediaRecorder available sizes: ${mediaRecorderSizes.toList()}")

                return Config(cameraId, previewSize, mediaRecorderSizes)
            }
        }

        throw CameraNotFoundException()
    }



    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Size {
        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight
            ) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        return when {
            bigEnough.size > 0 -> {
                Collections.min(
                    bigEnough,
                    CompareSizesByArea()
                )
            }
            notBigEnough.size > 0 -> {
                Collections.max(
                    notBigEnough,
                    CompareSizesByArea()
                )
            }
            else -> {
                Timber.e("Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }
}