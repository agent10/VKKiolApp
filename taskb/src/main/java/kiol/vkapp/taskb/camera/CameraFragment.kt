package kiol.vkapp.taskb

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.fragment.app.Fragment
import com.google.zxing.ResultPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.camera.*
import ru.timepad.domain.qr.QRBarRecognizer
import timber.log.Timber
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.*

class CameraFragment : Fragment(R.layout.camera_fragment_layout),
    ActivityCompat.OnRequestPermissionsResultCallback {

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (isCameraPermissionGranted()) {
                openCamera()
            } else {
                requestCameraPermissions()
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform()
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    }

    private lateinit var textureView: TextureView

    private var captureSession: CameraCaptureSession? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var cameraDevice: CameraDevice? = null

    private lateinit var cameraConfig: CameraConfigurator.Config

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            this@CameraFragment.cameraDevice = cameraDevice
            createCameraPreviewSession(cameraDevice)
            cameraOpenCloseLock.release()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Timber.d("camera onDisconnected happended: $cameraDevice")
            //if onDisconnected/onError/onClosed happened during open - we should release lock to make sure that closing will be fine
            cameraOpenCloseLock.release()
            closeCamera()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            Timber.e("camera onError happended: $error")
            onDisconnected(cameraDevice)
            this@CameraFragment.activity?.finish()
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            Timber.d("camera onClosed happened: $cameraDevice")
            onDisconnected(cameraDevice)
        }
    }

    private var backgroundThread: HandlerThread? = null

    private var backgroundHandler: Handler? = null

    private val cameraOpenCloseLock = Semaphore(1)

    private lateinit var imageReader: ImageReader

    private lateinit var qrBarRecognizer: QRBarRecognizer

    private lateinit var qrOverlay: QrOverlay

    private lateinit var mediaRecorder: MediaRecorder

    private val disposable = CompositeDisposable()

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var isVideoRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraConfigurator = CameraConfigurator(requireContext().applicationContext)

        scaleGestureDetector =
            ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector?): Boolean {
                    val scaleFactor = detector?.scaleFactor
                    scaleFactor?.let {
                        zLevel -= (1.0f - scaleFactor)
                        zLevel = max(min(zLevel, 0.75f), 0f)
                        setZoom(zLevel)
                    }
                    Timber.d("kiol scaleFactor: $scaleFactor, zLevel: $zLevel")
                    return true
                }
            })

        mediaRecorder = MediaRecorder()

        val app = context?.applicationContext as TheApp
        qrBarRecognizer = app.qrBarRecognizer
    }

    data class QrMyResult(val rect: RectF?, val angle: Float, val kX: Float, val kY: Float, val points: List<ResultPoint>? = null)

    var zLevel = 0.0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture)
        textureView.setOnTouchListener { v, event ->
            //            scaleGestureDetector.onTouchEvent(event)
            if (isVideoRecording) {
                //                stopCameraVideoRecording()
            } else {
                Toast.makeText(requireContext(), "Start media recorder", Toast.LENGTH_SHORT).show()
                isVideoRecording = true
                mediaRecorder.start()
            }
            false
        }
        qrOverlay = view.findViewById(R.id.qrOverlay)

        //        val d1 = Flowable.interval(50, TimeUnit.MILLISECONDS).map {
        //            rotationSensor.updateOrientationAngles()
        //        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe {
        //            qrOverlay.drawTestQr(it)
        //        }

        val d = qrBarRecognizer.subscribe().map {
            val points = it.points
            if (points.size == 4) {

                val t = System.currentTimeMillis()

                val arr = arrayOf(
                    points[0].x, points[0].y,
                    points[1].x, points[1].y,
                    points[2].x, points[2].y,
                    points[3].x, points[3].y
                )

                val ar = FloatArray(8)
                arr.forEachIndexed { index, fl ->
                    ar.set(index, fl)
                }
                val m = Matrix()
                m.preRotate(90f)
                m.preTranslate(0f, -480f)
                m.mapPoints(ar)

                val lastRects = arrayListOf(
                    ResultPoint(ar[0], ar[1]),
                    ResultPoint(ar[2], ar[3]),
                    ResultPoint(ar[4], ar[5]),
                    ResultPoint(ar[6], ar[7])
                )

                var xa = lastRects!!.sumByDouble {
                    it.x.toDouble()
                }.toFloat() / 4f

                var ya = lastRects!!.sumByDouble {
                    it.y.toDouble()
                }.toFloat() / 4f

                var r = sqrt((xa - lastRects!![0].x).pow(2) + (ya - lastRects!![0].y).pow(2))

                val lastRect = RectF(xa - r, ya - r, xa + r, ya + r)

                val rv = Vector(xa, ya, lastRect!!.right, lastRect!!.top)
                val lv = Vector(xa, ya, lastRects!![0].x, lastRects!![0].y)

                val v = acos(scalar(rv, lv))
                val z = rv.vec.x * lv.vec.y - rv.vec.y * lv.vec.x

                Timber.d("scalar: $v")
                val cosa = v * sign(z) * 180 / Math.PI.toFloat()


                Timber.d("qr coords time: ${System.currentTimeMillis() - t}")
                QrMyResult(lastRect, cosa, koefX, koefY, lastRects)
            } else {
                QrMyResult(null, 0f, koefX, koefY)
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            qrOverlay.drawQr2(it)
        }, {
            Timber.e("Recognize error")
        })

        //        disposable.add(sharedMainViewModel.uiEvents.subscribe {
        //            if (it is SharedMainViewModel.UIEvent.FlashTurn) {
        //                Timber.w("Flash switch todo")
        //                setTorch()
        //            }
        //        })

        //        view.findViewById<Button>(R.id.sysSettingBtn).setOnClickListener {
        //            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        //            intent.data = Uri.fromParts("package", activity!!.packageName, null)
        //            startActivity(intent)
        //        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    private var isTorchEnabled = false

    private fun setTorch() {
        isTorchEnabled = !isTorchEnabled
        if (isCameraPermissionGranted()) {
            previewRequestBuilder?.let { builder ->
                builder.set(
                    CaptureRequest.FLASH_MODE,
                    if (isTorchEnabled) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                )
                val previewRequest = builder.build()
                captureSession?.setRepeatingRequest(previewRequest, null, backgroundHandler)
            }
        }
    }

    private fun setZoom(level: Float) {
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)

            // We don't use a front facing camera in this sample.
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection != null &&
                cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
            ) {
                continue
            }

            val rect = RectF(characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE))
            val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            rect?.let {
                val m = Matrix()
                m.preScale(1f - level, 1f - level, rect.width() / 2f, rect.height() / 2f)
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

    private fun requestCameraPermissions() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)
    }

    private fun setNoPermissionsStub(enabled: Boolean) {
        view?.findViewById<View>(R.id.noPermissions)?.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            if (isCameraPermissionGranted()) {
                openCamera()
            } else {
                setNoPermissionsStub(true)
            }
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    private fun isCameraPermissionGranted() =
        ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    activity!!,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()

        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            Timber.d("perms, onRequestPermissionsResult, grantResults: $grantResults, permissions: $permissions")

            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                val shouldRational =
                    shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
                            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                Timber.d("perms, requestCameraPermission, shouldRational: $shouldRational")
                if (shouldRational) {
                    AlertDialog.Builder(context, R.style.PermissionRationalDialog)
                        .setMessage(getString(R.string.permission_rational))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            parentFragment?.requestPermissions(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                                1
                            )
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            setNoPermissionsStub(true)
                        }
                        .create().show()
                } else {
                    setNoPermissionsStub(true)
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private var koefX = 0f
    private var koefY = 0f
    private lateinit var cameraConfigurator: CameraConfigurator


    private fun createCameraConfig() {
        val displaySize = Point()
        activity!!.windowManager.defaultDisplay.getSize(displaySize)

        cameraConfig = cameraConfigurator.createConfig(displaySize, textureView.width, textureView.height, false)

        koefX = textureView.width / MAX_ANALYSIS_HEIGHT.toFloat()
        koefY = textureView.height / MAX_ANALYSIS_WIDTH.toFloat()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (!isCameraPermissionGranted()) {
            return
        }
        setNoPermissionsStub(false)
        createCameraConfig()
        configureTransform()
        val manager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraConfig.cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Timber.e(e)
        }

    }

    private fun setupImageReader() {
        imageReader =
            ImageReader.newInstance(
                MAX_ANALYSIS_WIDTH,
                MAX_ANALYSIS_HEIGHT,
                ImageFormat.YUV_420_888,
                2
            )

        imageReader.setOnImageAvailableListener({ reader ->
            val img = reader.acquireLatestImage()
            img?.let {
                val buf = img.planes[0].buffer
                val data = ByteArray(buf.remaining())
                buf.get(data)
                val w = img.width
                val h = img.height
                img.close()

                qrBarRecognizer.recognize(QRBarRecognizer.ImageData(data, w, h))
            }
        }, backgroundHandler)

    }

    private fun startCameraRecordingSession(cameraDeviceInternal: CameraDevice) {
        try {
            setupMediaRecorder()
            setupImageReader()

            val texture = textureView.surfaceTexture
            texture.setDefaultBufferSize(cameraConfig.previewSize.width, cameraConfig.previewSize.height)

            val surface = Surface(texture)
            val recorderSurface = mediaRecorder.surface
            val imgSurface = imageReader.surface

            previewRequestBuilder = cameraDeviceInternal.createCaptureRequest(
                CameraDevice.TEMPLATE_RECORD
            )
            previewRequestBuilder?.addTarget(surface)
            previewRequestBuilder?.addTarget(recorderSurface)
            previewRequestBuilder?.addTarget(imgSurface)

            cameraDeviceInternal.createCaptureSession(
                listOf(surface, recorderSurface, imgSurface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        Timber.d("createCaptureSession onConfigured started")

                        captureSession = cameraCaptureSession
                        try {
                            previewRequestBuilder?.let { builder ->
                                // Auto focus should be continuous for camera preview.
                                builder.set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )
                                builder.set(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON
                                )

                                val previewRequest = builder.build()
                                captureSession?.setRepeatingRequest(
                                    previewRequest,
                                    null, backgroundHandler
                                )

                                Timber.d("createCaptureSession onConfigured finished")
                            }

                        } catch (e: CameraAccessException) {
                            Timber.e(e)
                        } catch (e: IllegalStateException) {
                            Timber.e(e)
                        }
                    }

                    override fun onClosed(session: CameraCaptureSession) {
                        super.onClosed(session)
                        Timber.d("onConfigureFailed")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        //                        activity.showToast("Failed")
                        Timber.e("onConfigureFailed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Timber.e(e)
        } catch (e: IllegalStateException) {
            Timber.e(e)
            Toast.makeText(context, R.string.camera_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun createCameraPreviewSession(cameraDeviceInternal: CameraDevice) {
        startCameraRecordingSession(cameraDeviceInternal)
    }

    private fun configureTransform() {
        activity ?: return
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        textureView.configureTransform(rotation, cameraConfig.previewSize)
    }

    private fun chooseCamcorderProfile(cameraDevice: CameraDevice?): CamcorderProfile {
        var quality = CamcorderProfile.QUALITY_LOW
        cameraDevice?.let {
            if (cameraConfig.mediaRecorderSize.contains(Size(1280, 720))) {
                quality = CamcorderProfile.QUALITY_720P
            } else if (cameraConfig.mediaRecorderSize.contains(Size(720, 480))) {
                quality = CamcorderProfile.QUALITY_480P
            }
        }
        return CamcorderProfile.get(quality)
    }

    private fun setupMediaRecorder() {
        val profile = chooseCamcorderProfile(cameraDevice)
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setOutputFile(requireContext().filesDir.absolutePath + "/myvideo.mp4")
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate)
        mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate)
        mediaRecorder.setAudioSamplingRate(profile.audioSampleRate)
        mediaRecorder.setVideoFrameRate(profile.videoFrameRate)
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setOrientationHint(90)
        //        val rotation = activity?.getWindowManager()?.defaultDisplay?.rotation
        //        switch(mSensorOrientation) {
        //            case SENSOR_ORIENTATION_DEFAULT_DEGREES :
        //            mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
        //            break;
        //            case SENSOR_ORIENTATION_INVERSE_DEGREES :
        //            mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
        //            break;
        //        }
        mediaRecorder.prepare()
    }

    private fun resetMediaRecorder() {
        mediaRecorder.stop()
        mediaRecorder.reset()
        isVideoRecording = false
    }

    companion object {

        private val ORIENTATIONS = SparseIntArray()

        private const val MAX_ANALYSIS_WIDTH = 640
        private const val MAX_ANALYSIS_HEIGHT = 480

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        @JvmStatic
        fun newInstance(): CameraFragment =
            CameraFragment()
    }
}