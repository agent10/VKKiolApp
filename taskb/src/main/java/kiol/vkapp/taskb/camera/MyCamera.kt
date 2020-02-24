package kiol.vkapp.taskb.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import kiol.vkapp.taskb.camera.MyCamera.CameraType.Back
import kiol.vkapp.taskb.camera.MyCamera.CameraType.Front
import timber.log.Timber

class MyCamera(private val context: Context) {

    enum class CameraType {
        Back, Front
    }

    private var cameraType = Back

    var camSwitchFinished: (cameraType: CameraType) -> Unit = {}

    var onCamRecord: (isRecord: Boolean) -> Unit = {}

    private val captureSessionCreator = CaptureSessionCreator(context) {
        isCamChanging = false
        uiHandler.post {
            camSwitchFinished(cameraType)
        }
    }

    private val cameraConfigurator = CameraConfigurator(context)

    private val backgroundThread = HandlerThread("MyCameraThread").apply {
        start()
    }
    private val uiHandler = Handler()
    private val backgroundHandler = Handler(backgroundThread.looper)

    private lateinit var textureView: TextureView

    @Volatile
    var isCamChanging = false

    var isRecording = false
        get() = mediaRecorder.isRecording

    private val recognizer = Recognizer(context, backgroundHandler)
    private val mediaRecorder = MediaRecorderInternal(context)
    private val torch = Torch(backgroundHandler)

    private var cameraDevice: CameraDevice? = null


    private val zoomer = Zoomer(context, backgroundHandler)

    private fun changeCamera(cameraType: CameraType) {
        if (this.cameraType != cameraType) {
            this.cameraType = cameraType

            onStop {
                uiHandler.post {
                    checkTextureView()
                }
            }
        }
    }

    fun switchCamera() {
        backgroundHandler.post {
            isCamChanging = true
            when (cameraType) {
                Back -> changeCamera(Front)
                Front -> changeCamera(Back)
            }
        }
    }

    fun setTorch(on: Boolean) {
        if (!isCamChanging) {
            torch.setEnabled(on)
        }
    }

    fun isTorchEnabled() = torch.isTorchEnabled

    fun startRecord() {
        backgroundHandler.post {
            mediaRecorder.record()
            onCamRecord(true)
        }
    }

    fun stopRecord() {
        backgroundHandler.post {
            mediaRecorder.stop()
            onCamRecord(false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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
        this.textureView.setOnTouchListener { v, event ->
            zoomer.scaleGestureDetector.onTouchEvent(event)
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
    fun onStop(onClosed: () -> Unit = {}) {
        Timber.d("onStop")
        backgroundHandler.post {
            mediaRecorder.stop()
            torch.reset()

            cameraDevice?.close()
            cameraDevice = null
            onClosed()
        }
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
            Back -> CaptureSessionCreator.BackCameraSessionStrategy(zoomer, recognizer.imageReader, mediaRecorder, torch)
            Front -> CaptureSessionCreator.FrontCameraSessionStrategy(zoomer, mediaRecorder, torch)
        }
        captureSessionCreator.create(sessionStrategy, camera, textureView, cameraConfig, backgroundHandler)
    }

    fun setZoom(zoomLevel: Float) {
        zoomer.setZoom(zoomLevel)
    }
}