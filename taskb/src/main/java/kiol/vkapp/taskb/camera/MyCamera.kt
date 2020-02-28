package kiol.vkapp.taskb.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.R
import kiol.vkapp.taskb.camera.MyCamera.CameraType.Back
import kiol.vkapp.taskb.camera.MyCamera.CameraType.Front
import kiol.vkapp.taskb.camera.components.MediaRecorderInternal
import kiol.vkapp.taskb.camera.components.Recognizer
import kiol.vkapp.taskb.camera.components.Torch
import kiol.vkapp.taskb.camera.components.Zoomer
import kiol.vkapp.taskb.camera.ui.QrOverlay
import kiol.vkapp.taskb.camera.ui.configureTransform
import timber.log.Timber
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class MyCamera(private val context: Context, file: String) {

    enum class CameraState {
        Idle, Initializing, Ready, Closing
    }

    enum class CameraType {
        Back, Front
    }

    sealed class Record {
        object Start : Record()
        data class End(val timeMs: Long, val isStopped: Boolean) : Record()
    }

    companion object {
        private const val QR_WINDOW_MS = 5000L
        const val MIN_VALID_RECORD_TIME_MS = 1000L
    }

    @Volatile
    var cameraState: CameraState = CameraState.Idle

    var cameraType = Back
        private set

    var camSwitchFinished: (cameraType: CameraType) -> Unit = {}

    var onCamRecord: (isRecord: Record) -> Unit = { }

    private var qrListenerEnabled = true
    private val _qrListener: PublishProcessor<String> = PublishProcessor.create()

    private val captureSessionCreator = CaptureSessionCreator(context) {
        uiHandler.post {
            if (isCamHardWorking) {
                camSwitchFinished(cameraType)
            }
            isCamHardWorking = false
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
    var isCamHardWorking = false

    private val camSemaphore = Semaphore(1)

    private var recordStartTimestampMs = 0L

    private val recognizer = Recognizer(context, backgroundHandler, uiHandler) {
        if (qrListenerEnabled) {
            _qrListener.onNext(it)
        }
    }

    private val mediaRecorder = MediaRecorderInternal(file)
    private val torch = Torch(backgroundHandler)

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    private val zoomer = Zoomer(context, backgroundHandler)

    private fun changeCamera(cameraType: CameraType) {
        if (this.cameraType != cameraType) {
            this.cameraType = cameraType

            stopCamera {
                uiHandler.post {
                    checkTextureView()
                }
            }
        }
    }

    fun switchCamera() {
        if (cameraState == CameraState.Ready) {
            this.cameraType = cameraType

            backgroundHandler.post {
                isCamHardWorking = true
                when (cameraType) {
                    Back -> {
                        setEnableQrCallback(false)
                        changeCamera(Front)
                    }
                    Front -> {
                        setEnableQrCallback(true)
                        changeCamera(Back)
                    }
                }
            }
        }
    }

    fun setTorch(on: Boolean) {
        if (cameraState == CameraState.Ready) {
            if (!isCamHardWorking) {
                torch.setEnabled(on)
            }
        }
    }

    fun getQrListener(): Flowable<String> {
        return _qrListener.window(QR_WINDOW_MS, QR_WINDOW_MS, TimeUnit.MILLISECONDS).flatMap {
            it.distinctUntilChanged()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun isTorchEnabled() = torch.isTorchEnabled

    fun startRecord() {
        if (cameraState == CameraState.Ready) {
            if (!isCamHardWorking) {
                backgroundHandler.post {
                    isCamHardWorking = true
                    recordStartTimestampMs = System.currentTimeMillis()
                    mediaRecorder.record()
                    onCamRecord(Record.Start)
                    isCamHardWorking = false

                }
            }
        }
    }

    fun stopRecord() {
        if (cameraState == CameraState.Ready) {
            if (!isCamHardWorking) {
                backgroundHandler.post {
                    isCamHardWorking = true
                    val time = System.currentTimeMillis() - recordStartTimestampMs
                    uiHandler.post {
                        onCamRecord(Record.End(time, false))
                    }
                    stopCamera {
                        uiHandler.post {
                            checkTextureView()
                        }
                        isCamHardWorking = false
                    }
                }
            }
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
            if (cameraState == CameraState.Ready) {
                zoomer.scaleGestureDetector.onTouchEvent(event)
            }
            false
        }
        checkTextureView()
    }

    fun setQROverlay(qrOverlay: QrOverlay) {
        recognizer.setViews(qrOverlay)
    }

    fun startCamera() {
        Timber.d("onStart")
        checkTextureView()
    }

    @Synchronized
    fun stopCamera() {
        Timber.d("onStop")
        recognizer.release()
        stopCamera()
    }

    @Synchronized
    private fun stopCamera(onClosed: () -> Unit = {}) {
        Timber.d("stopCamera")
        backgroundHandler.post {
            mediaRecorder.stop()
            torch.reset()

            Timber.d("try close camera device")
            cameraDevice?.let {
                cameraDevice?.close()
                cameraDevice = null

                camSemaphore.acquire()
            }

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
            if (camSemaphore.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                manager.openCamera(cameraConfig.cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        Timber.d("onOpened")
                        cameraDevice = camera
                        startCameraSession(camera, cameraConfig)
                        camSemaphore.release()
                    }

                    override fun onClosed(camera: CameraDevice) {
                        Timber.d("onClosed")

                        super.onClosed(camera)
                        camSemaphore.release()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Timber.d("onDisconnected")
                        camSemaphore.release()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Timber.d("onError, e: $error")
                        camSemaphore.release()
                    }

                }, backgroundHandler)
            } else {
                Timber.e("Can't open camera, still waiting for close...")
                Toast.makeText(context, R.string.camera_open_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    @Synchronized
    private fun startCameraSession(camera: CameraDevice, cameraConfig: CameraConfigurator.Config) {
        val sessionStrategy = when (cameraType) {
            Back -> CaptureSessionCreator.BackCameraSessionStrategy(
                zoomer,
                recognizer.imageReader,
                mediaRecorder,
                torch,
                recognizer
            )
            Front -> CaptureSessionCreator.FrontCameraSessionStrategy(zoomer, mediaRecorder, torch, recognizer)
        }
        captureSessionCreator.create(sessionStrategy, camera, textureView, cameraConfig, backgroundHandler) {
            cameraCaptureSession = it
        }
    }

    fun setZoom(zoomLevel: Float) {
        if (cameraState == CameraState.Ready) {
            try {
                zoomer.setZoom(zoomLevel)
            } catch (e: Exception) {
                Timber.e("Zoom failed: $e")
            }
        }
    }

    fun setEnableQrCallback(value: Boolean) {
        qrListenerEnabled = value
        Timber.d("setEnableQrCallback: $value")
    }
}