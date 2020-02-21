package kiol.vkapp.taskb

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.fragment.app.Fragment
import com.google.zxing.ResultPoint
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.camera.QrOverlay
import kiol.vkapp.taskb.camera.Vector
import kiol.vkapp.taskb.camera.scalar
import kiol.vkapp.taskb.camera.sensors.RotationSensor
import ru.timepad.domain.qr.QRBarRecognizer
import timber.log.Timber
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.*

class CameraFragment : Fragment(R.layout.camera_fragment_layout),
    ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (isCameraPermissionGranted()) {
                openCamera(textureView.width, textureView.height)
            } else {
                requestCameraPermissions()
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    }

    /**
     * ID of the current [CameraDevice].
     */
    private lateinit var cameraId: String

    /**
     * An [AutoFitTextureView] for camera preview.
     */
    private lateinit var textureView: AutoFitTextureView

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var captureSession: CameraCaptureSession? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private lateinit var previewSize: Size

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            this@CameraFragment.cameraDevice = cameraDevice
            createCameraPreviewSession(cameraDevice)
            //try to release lock after camera was fully configured
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

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    private lateinit var imageReader: ImageReader

    private lateinit var qrBarRecognizer: QRBarRecognizer

    private lateinit var qrOverlay: QrOverlay

    private lateinit var mediaRecorder: MediaRecorder

    //    @Inject
    //    lateinit var sharedMainViewModel: SharedMainViewModel

    private val disposable = CompositeDisposable()

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var isVideoRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    data class QrMyResult(val rect: RectF?, val angle: Float, val kX: Float, val kY: Float)

    var zLevel = 0.0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture)
        textureView.setOnTouchListener { v, event ->
            //            scaleGestureDetector.onTouchEvent(event)
            if (isVideoRecording) {
                stopCameraVideoRecording()
            } else {
                backgroundHandler?.post {
                    startCameraRecordingSession(cameraDevice!!)
                }
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
                QrMyResult(lastRect, cosa, koefX, koefY)
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
                openCamera(textureView.width, textureView.height)
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


    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraDirection != null &&
                    cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
                ) {
                    continue
                }

                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displaySize = Point()
                activity!!.windowManager.defaultDisplay.getSize(displaySize)

                val swappedDimensions = displaySize.x < displaySize.y

                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height
                val maxPreviewWidth = rotatedPreviewWidth
                val maxPreviewHeight = rotatedPreviewHeight

                previewSize =
                    chooseOptimalSize(
                        map.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewWidth, rotatedPreviewHeight,
                        maxPreviewWidth, maxPreviewHeight,
                        Size(maxPreviewWidth, maxPreviewHeight)
                    )

                val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                Timber.d("hardwareLevel: $hardwareLevel")

                val yuvAvailableSizes = map.getOutputSizes(ImageFormat.YUV_420_888)
                Timber.d("YUV available sizes: ${yuvAvailableSizes.toList()}")

                imageReader =
                    ImageReader.newInstance(
                        MAX_ANALYSIS_WIDTH,
                        MAX_ANALYSIS_HEIGHT,
                        ImageFormat.YUV_420_888,
                        2
                    )

                koefX = displaySize.x.toFloat() / MAX_ANALYSIS_HEIGHT.toFloat()
                koefY = displaySize.y.toFloat() / MAX_ANALYSIS_WIDTH.toFloat()

                this.cameraId = cameraId

                // We've found a viable camera and finished setting up member variables,
                // so we don't need to iterate through other available cameras.
                return
            }
        } catch (e: CameraAccessException) {
            Timber.e(e)
        } catch (e: NullPointerException) {
            Timber.e(e)
        }
    }


    /**
     * Opens the camera specified by [CameraFragment.cameraId].
     */
    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        if (!isCameraPermissionGranted()) {
            return
        }
        setNoPermissionsStub(false)

        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val manager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    /**
     * Closes the current [CameraDevice].
     */
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

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
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

    private fun startCameraPreviewSession(cameraDeviceInternal: CameraDevice) {
        try {
            val texture = textureView.surfaceTexture

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

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)
            val imgSurface = imageReader.surface

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDeviceInternal.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder?.addTarget(surface)
            previewRequestBuilder?.addTarget(imgSurface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDeviceInternal.createCaptureSession(
                Arrays.asList(surface, imgSurface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        Timber.d("createCaptureSession onConfigured started")

                        // When the session is ready, we start displaying the preview.
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

                                // Finally, we start displaying the camera preview.
                                val previewRequest = builder.build()
                                captureSession?.setRepeatingRequest(
                                    previewRequest,
                                    null, backgroundHandler
                                )
                                Timber.d("createCaptureSession onConfigured finished")
                                activity?.runOnUiThread {
                                    Toast.makeText(requireContext(), "Start qr mode", Toast.LENGTH_SHORT).show()
                                }
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

    private fun stopCameraPreviewSession() {
        captureSession?.close()
        captureSession = null
    }

    private fun startCameraRecordingSession(cameraDeviceInternal: CameraDevice) {
        try {
            stopCameraPreviewSession()
            setupMediaRecorder()

            val texture = textureView.surfaceTexture

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            val recorderSurface = mediaRecorder.surface

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDeviceInternal.createCaptureRequest(
                CameraDevice.TEMPLATE_RECORD
            )
            previewRequestBuilder?.addTarget(surface)
            previewRequestBuilder?.addTarget(recorderSurface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDeviceInternal.createCaptureSession(
                Arrays.asList(surface, recorderSurface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        Timber.d("createCaptureSession onConfigured started")

                        // When the session is ready, we start displaying the preview.
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

                                // Finally, we start displaying the camera preview.
                                val previewRequest = builder.build()
                                captureSession?.setRepeatingRequest(
                                    previewRequest,
                                    null, backgroundHandler
                                )

                                isVideoRecording = true
                                mediaRecorder.start()

                                Timber.d("createCaptureSession onConfigured finished")
                                activity?.runOnUiThread {
                                    Toast.makeText(requireContext(), "Start recording mode", Toast.LENGTH_SHORT).show()
                                }
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

    private fun stopCameraVideoRecording() {
        captureSession?.close()
        captureSession = null
        resetMediaRecorder()
        backgroundHandler?.post {
            cameraDevice?.let {
                startCameraPreviewSession(it)
            }
        }
    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession(cameraDeviceInternal: CameraDevice) {
        startCameraPreviewSession(cameraDeviceInternal)
    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.width.toFloat(), previewSize.height.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            with(matrix) {
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }

        val bufferRatio = previewSize.height / previewSize.width.toFloat()

        val scaledWidth: Float
        val scaledHeight: Float
        // Match longest sides together -- i.e. apply center-crop transformation
        if (viewRect.width() > viewRect.height()) {
            scaledHeight = viewRect.width()
            scaledWidth = viewRect.width() * bufferRatio
        } else {
            scaledHeight = viewRect.height()
            scaledWidth = viewRect.height() * bufferRatio
        }

        // Compute the relative scale value
        val xScale = scaledWidth / viewRect.width()
        val yScale = scaledHeight / viewRect.height()

        // Scale input buffers to fill the view finder
        matrix.preScale(xScale, yScale, centerX, centerY)

        matrix.mapRect(viewRect)

        val scale = if (viewRect.width().toFloat() * viewHeight.toFloat() > viewWidth.toFloat() * viewRect.height().toFloat()) {
            viewHeight.toFloat() / viewRect.height().toFloat()
        } else {
            viewWidth.toFloat() / viewRect.width().toFloat()
        }

        matrix.preScale(scale, scale, centerX, centerY)
        textureView.setTransform(matrix)
    }

    private fun setupMediaRecorder() {
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
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

        val d = Flowable.fromCallable {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(requireContext().filesDir.absolutePath + "/myvideo.mp4")
            for (i in 0 until mediaExtractor.trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(i)
                val w = trackFormat.getInteger(MediaFormat.KEY_WIDTH)
                val h = trackFormat.getInteger(MediaFormat.KEY_HEIGHT)
                val dur = trackFormat.getLong(MediaFormat.KEY_HEIGHT)

                val a = 10
            }

//            val mediaMetadataRetriever = MediaMetadataRetriever()
//            mediaMetadataRetriever.setDataSource(requireContext().filesDir.absolutePath + "/myvideo.mp4")
//
//            val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
//
//            mediaMetadataRetriever.getScaledFrameAtTime()
        }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe({

        }, {

        })
    }

    companion object {

        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()
        private val FRAGMENT_DIALOG = "dialog"

        private val MAX_ANALYSIS_WIDTH = 640
        private val MAX_ANALYSIS_HEIGHT = 480

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * Tag for the [Log].
         */
        private val TAG = "CameraFragment"

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended
         *                          output class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
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

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(
                    bigEnough,
                    CompareSizesByArea()
                )
            } else if (notBigEnough.size > 0) {
                return Collections.max(
                    notBigEnough,
                    CompareSizesByArea()
                )
            } else {
                Timber.e("Couldn't find any suitable preview size")
                return choices[0]
            }
        }

        @JvmStatic
        fun newInstance(): CameraFragment =
            CameraFragment()
    }
}