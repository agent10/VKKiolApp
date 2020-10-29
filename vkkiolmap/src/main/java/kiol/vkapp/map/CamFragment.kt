package kiol.vkapp.map

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kiol.vkapp.commonui.permissions.PermissionManager
import kiol.vkapp.commonui.viewLifecycleLazy
import kiol.vkapp.map.databinding.CamLayoutBinding
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CamFragment : Fragment(R.layout.cam_layout) {
    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    interface OnPictureListener {
        fun onTaken(uri: Uri)
    }

    private var imageCapture: ImageCapture? = null

    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private lateinit var permissionManager: PermissionManager

    val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {

            val v1 = detector?.scaleFactor ?: 0.0f
            val v2 = cameraInfo?.zoomState?.value?.linearZoom ?: 0f

            cameraControl?.setLinearZoom(v2 - (1.0f - v1))
            return true
        }
    })

    private val binding by viewLifecycleLazy {
        CamLayoutBinding.bind(requireView())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(requireContext(), listOf(Manifest.permission.CAMERA))
    }

    override fun onStart() {
        super.onStart()
        permissionManager.checkPermissions(this) {
            binding.noPermissionsLayout.visibility = if (it) View.GONE else View.VISIBLE
            if (it) {
                startCamera()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cameraCaptureButton.setOnClickListener {
            binding.cameraCaptureButton.postDelayed({ takePhoto() }, RecordButton.ScaleDuration)
        }

        binding.torchSwitcher.setOnClickListener {
            cameraControl?.enableTorch(binding.torchSwitcher.isChecked)
        }

        binding.viewFinder.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            imageCapture = ImageCapture.Builder().setTargetResolution(Size(480, 640)).build()
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
            } catch (e: Exception) {
                Timber.e("Use case bind failed: $e")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        setCaptureState(true)

        val imageCapture = imageCapture ?: return
        val photoFile = File(
            requireActivity().filesDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    setCaptureState(false)
                    Timber.e("Photo capture failed: ${exc.message}")
                    Toast.makeText(requireContext(), R.string.cam_error, Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    setCaptureState(false)
                    val uri = Uri.fromFile(photoFile)

                    (parentFragment as OnPictureListener).onTaken(uri)
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionManager.invokePermissionRequest(requestCode, permissions, grantResults)
    }

    private fun setCaptureState(isCapture: Boolean) {
        binding.camSwitchProgress.visibility = if (isCapture) View.VISIBLE else View.GONE
        binding.cameraCaptureButton.isEnabled = !isCapture
        binding.torchSwitcher.isEnabled = !isCapture
    }
}