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

private var CAM_GLOBAL_ID = 0
private fun getGlobalCamId(): String {
    return ("MyCamId$CAM_GLOBAL_ID").also { CAM_GLOBAL_ID++ }
}

class MyCamera(private val context: Context, file: String) {

    enum class CameraState {
        Idle, Initializing, Ready, Closing, Closed, Error
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
        const val MIN_VALID_RECORD_TIME_MS = 2000L
    }

    private val camGlobalId = getGlobalCamId()

    @Volatile
    var cameraState: CameraState = CameraState.Idle
        private set(value) {
            Timber.d("Change camera($camGlobalId) state from $cameraState to $value")
            field = value
            uiHandler.post {
                onCamStateChanged(value)
            }
        }

    var cameraType = Back

    var onCamRecord: (isRecord: Record) -> Unit = { }

    var onCamStateChanged: (CameraState) -> Unit = {}

    private var qrListenerEnabled = true
    private val _qrListener: PublishProcessor<String> = PublishProcessor.create()

    private val captureSessionCreator = CaptureSessionCreator(context)

    private val cameraConfigurator = CameraConfigurator(context)

    private val backgroundThread = HandlerThread("MyCameraThread").apply {
        start()
    }
    private val uiHandler = Handler()
    private val backgroundHandler = Handler(backgroundThread.looper)

    private lateinit var textureView: TextureView

    private var recordStartTimestampMs = 0L

    private val recognizer = Recognizer(context, backgroundHandler, uiHandler) {
        if (qrListenerEnabled) {
            _qrListener.onNext(it)
        }
    }

    private val mediaRecorder = MediaRecorderInternal(file)
    private val torch = Torch(backgroundHandler)

    private var cameraDevice: CameraDevice? = null

    private val zoomer = Zoomer(context, backgroundHandler)

    init {
        Timber.d("Camera($camGlobalId) constructed with $cameraState state")
    }

    fun switchTorch(): Boolean {
        if (cameraState == CameraState.Ready) {
            torch.setEnabled(!isTorchEnabled())
            return true
        }
        return false
    }

    fun getQrListener(): Flowable<String> {
        return _qrListener.window(QR_WINDOW_MS, QR_WINDOW_MS, TimeUnit.MILLISECONDS).flatMap {
            it.distinctUntilChanged()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun isTorchEnabled() = torch.isTorchEnabled

    fun startRecord() {
        if (cameraState == CameraState.Ready) {
            backgroundHandler.post {
                recordStartTimestampMs = System.currentTimeMillis()
                mediaRecorder.record()
                onCamRecord(Record.Start)
            }
        }
    }

    fun stopRecord() {
        if (cameraState == CameraState.Ready) {
            backgroundHandler.post {
                mediaRecorder.stop()
                val time = System.currentTimeMillis() - recordStartTimestampMs
                uiHandler.post {
                    onCamRecord(Record.End(time, false))
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
                textureAvailableCallback(surface)
            }
        }
        this.textureView.setOnTouchListener { v, event ->
            if (cameraState == CameraState.Ready) {
                zoomer.scaleGestureDetector.onTouchEvent(event)
                return@setOnTouchListener true
            }
            false
        }
    }

    fun setQROverlay(qrOverlay: QrOverlay) {
        recognizer.setViews(qrOverlay)
    }

    private var textureAvailableCallback: (SurfaceTexture?) -> Unit = {}
    private fun waitForSurface() {
        cameraState = CameraState.Initializing
        textureAvailableCallback = {}
        if (textureView.isAvailable) {
            val st = textureView.surfaceTexture
            if (st != null) {
                setupCamera()
            } else {
                cameraState = CameraState.Error
            }
        } else {
            textureAvailableCallback = { st ->
                if (st != null) {
                    setupCamera()
                } else {
                    cameraState = CameraState.Error
                }
                textureAvailableCallback = {}
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupCamera() {
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
                Timber.d("onOpened($camGlobalId)")
                cameraDevice = camera
                try {
                    startCameraSession(camera, cameraConfig) {
                        cameraState = CameraState.Ready
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    cameraState = CameraState.Error
                }
            }

            override fun onClosed(camera: CameraDevice) {
                super.onClosed(camera)
                cameraState = CameraState.Closed
                Timber.d("onClosed($camGlobalId)")
            }

            override fun onDisconnected(camera: CameraDevice) {
                Timber.d("onDisconnected($camGlobalId)")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Timber.d("onError($camGlobalId), e: $error")
                cameraState = CameraState.Error
            }
        }, backgroundHandler)
    }

    @Synchronized
    fun startCamera() {
        Timber.d("try startCamera($camGlobalId)")
        if (cameraState == CameraState.Idle) {
            Timber.d("startCamera($camGlobalId)")
            waitForSurface()
        }
    }

    @Synchronized
    fun stopCamera() {
        Timber.d("try stopCamera($camGlobalId)")
        recognizer.release()
        if (cameraState == CameraState.Ready) {
            cameraState = CameraState.Closing
            stopCameraForce()
        } else if (cameraState == CameraState.Error) {
            Timber.d("try stopCamera($camGlobalId) on Error state")
            cameraState = CameraState.Closing
            stopCameraForce()
        }
    }

    private fun stopCameraForce() {
        Timber.d("stopCameraForce($camGlobalId)")

        backgroundHandler.post {
            mediaRecorder.stop()
            torch.reset()

            backgroundHandler.removeCallbacksAndMessages(null)

            Timber.d("try close camera($camGlobalId) device: $cameraDevice")
            cameraDevice?.let {
                cameraDevice?.close()
                cameraDevice = null
            }
        }
    }

    @Synchronized
    private fun startCameraSession(
        camera: CameraDevice,
        cameraConfig: CameraConfigurator.Config,
        onCameraSession: (CameraCaptureSession) -> Unit = {}
    ) {
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
        captureSessionCreator.create(
            sessionStrategy, camera,
            textureView, cameraConfig, backgroundHandler, onCameraSession
        )
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