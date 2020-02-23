package kiol.vkapp.taskb.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import kiol.vkapp.taskb.R
import kotlinx.android.synthetic.main.camera_container_fragment.*
import timber.log.Timber
import kotlin.math.abs

class CameraContainerFragment : Fragment(R.layout.camera_container_fragment) {

    companion object {
        private const val PermissionRequestCode = 42
    }

    private lateinit var myCamera: MyCamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myCamera = MyCamera(requireContext())
    }

    var lastDownY = -1f

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("MyCamera fragment onViewCreated")
        view.findViewById<TextureView>(R.id.cameraView).apply {
            myCamera.setTextureView(this)
        }

        view.findViewById<QrOverlay>(R.id.qrOverlay).apply {
            myCamera.setQROverlay(this)
        }

        view.findViewById<ImageButton>(R.id.camSwitcher).setOnClickListener {
            myCamera.switchCamera()
        }

        val recordBtn = view.findViewById<RecordButton>(R.id.recordBtn)
        recordBtn.callback = {
            myCamera.setZoom(it)
        }
        cameraView.doOnLayout {
            recordBtn.zoomHeight = it.measuredHeight.toFloat()
        }

        view.findViewById<ImageButton>(R.id.torchSwitcher).setOnClickListener {
            myCamera.setTorch(!myCamera.isTorchEnabled())
        }

        //        childFragmentManager.beginTransaction().replace(R.id.cameraContainer, CameraFragment.newInstance()).commit()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("MyCamera fragment onStart")
        if (isCameraPermissionGranted()) {
            myCamera.onStart()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), PermissionRequestCode)
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("MyCamera fragment onStop")
        myCamera.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == PermissionRequestCode) {
            Timber.d("perms, onRequestPermissionsResult, grantResults: $grantResults, permissions: $permissions")
            if (grantResults.all { it == 1 }) {
                myCamera.onStart()
            } else {

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun isCameraPermissionGranted() =
        ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    activity!!,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
}