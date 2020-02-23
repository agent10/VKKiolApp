package kiol.vkapp.taskb.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import com.google.zxing.ResultPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.CameraFragment
import kiol.vkapp.taskb.TheApp
import kiol.vkapp.taskb.camera.MyCamera.CameraType.Back
import kiol.vkapp.taskb.camera.MyCamera.CameraType.Front
import ru.timepad.domain.qr.QRBarRecognizer
import timber.log.Timber
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

class MyCamera(private val context: Context) {

    enum class CameraType {
        Back, Front
    }

    private var cameraType = Back

    private val captureSessionCreator = CaptureSessionCreator(context)
    private val cameraConfigurator = CameraConfigurator(context)

    private val backgroundThread = HandlerThread("MyCameraThread").apply {
        start()
    }
    private val backgroundHandler = Handler(backgroundThread.looper)

    private lateinit var textureView: TextureView

    var isRecording = false
        get() = mediaRecorder.isRecording

    private val recognizer = Recognizer(context, backgroundHandler)
    private val mediaRecorder = MediaRecorderInternal(context)
    private val torch = Torch(backgroundHandler)

    private var cameraDevice: CameraDevice? = null

    private fun changeCamera(cameraType: CameraType) {
        if (this.cameraType != cameraType) {
            this.cameraType = cameraType

            onStop()
            checkTextureView()
        }
    }

    fun switchCamera() {
        when (cameraType) {
            Back -> changeCamera(Front)
            Front -> changeCamera(Back)
        }
    }

    fun setTorch(on: Boolean) {
        torch.setEnabled(on)
    }

    fun isTorchEnabled() = torch.isTorchEnabled

    fun startRecord() {
        mediaRecorder.record()
    }

    fun stopRecord() {
        mediaRecorder.stop()
    }

    fun setTextureView(textureView: TextureView) {
        this.textureView = textureView
        this.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                checkTextureView()
            }
        }
        checkTextureView()
    }

    fun setQROverlay(qrOverlay: QrOverlay) {
        recognizer.setViews(qrOverlay)
    }

    fun onStart() {
        Timber.d("onStart")
        checkTextureView()
    }

    @Synchronized
    fun onStop() {
        Timber.d("onStop")
        mediaRecorder.stop()
        torch.reset()

        cameraDevice?.close()
        cameraDevice = null

    }

    @SuppressLint("MissingPermission")
    @Synchronized
    private fun checkTextureView() {
        if (textureView.isAvailable) {
            val cameraConfig = cameraConfigurator.createConfig(
                null,
                textureView.width,
                textureView.height,
                cameraType == Front
            )

            textureView.configureTransform(Surface.ROTATION_0, cameraConfig.previewSize)

            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.openCamera(cameraConfig.cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Timber.d("onOpened")
                    cameraDevice = camera
                    startCameraSession(camera, cameraConfig)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Timber.d("onDisconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Timber.d("onError, e: $error")
                }

            }, backgroundHandler)
        }
    }

    private fun startCameraSession(camera: CameraDevice, cameraConfig: CameraConfigurator.Config) {
        val sessionStrategy = when (cameraType) {
            Back -> CaptureSessionCreator.BackCameraSessionStrategy(recognizer.imageReader, mediaRecorder, torch)
            Front -> CaptureSessionCreator.FrontCameraSessionStrategy(mediaRecorder, torch)
        }
        captureSessionCreator.create(sessionStrategy, camera, textureView, cameraConfig, backgroundHandler)
    }
}