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
import android.media.ImageReader
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
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.camera.QrOverlay
import ru.timepad.domain.qr.QRBarRecognizer
import timber.log.Timber
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

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

    //    @Inject
    //    lateinit var sharedMainViewModel: SharedMainViewModel

    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = context?.applicationContext as TheApp
        qrBarRecognizer = app.qrBarRecognizer


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture)
        qrOverlay = view.findViewById(R.id.qrOverlay)

        val d = qrBarRecognizer.subscribe().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            qrOverlay.drawQr(it.points)
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

    private fun requestCameraPermissions() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
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
        ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

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
                val shouldRational = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                Timber.d("perms, requestCameraPermission, shouldRational: $shouldRational")
                if (shouldRational) {
                    AlertDialog.Builder(context, R.style.PermissionRationalDialog)
                        .setMessage(getString(R.string.permission_rational))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            parentFragment?.requestPermissions(
                                arrayOf(Manifest.permission.CAMERA),
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

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession(cameraDeviceInternal: CameraDevice) {
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