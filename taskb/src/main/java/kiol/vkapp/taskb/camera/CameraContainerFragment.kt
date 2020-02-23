package kiol.vkapp.taskb.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kiol.vkapp.taskb.R
import timber.log.Timber

class CameraContainerFragment : Fragment(R.layout.camera_container_fragment) {

    companion object {
        private const val PermissionRequestCode = 42
    }

    private lateinit var myCamera: MyCamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myCamera = MyCamera(requireContext())
    }

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

        view.findViewById<ImageButton>(R.id.recordBtn).setOnClickListener {
            if (myCamera.isRecording) {
                myCamera.stopRecord()
            } else {
                myCamera.startRecord()
            }
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

            //            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            //                val shouldRational =
            //                    shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
            //                            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            //                Timber.d("perms, requestCameraPermission, shouldRational: $shouldRational")
            //                if (shouldRational) {
            //                    AlertDialog.Builder(context, R.style.PermissionRationalDialog)
            //                        .setMessage(getString(R.string.permission_rational))
            //                        .setPositiveButton(android.R.string.ok) { _, _ ->
            //                            parentFragment?.requestPermissions(
            //                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            //                                1
            //                            )
            //                        }
            //                        .setNegativeButton(android.R.string.cancel) { _, _ ->
            //                            setNoPermissionsStub(true)
            //                        }
            //                        .create().show()
            //                } else {
            //                    setNoPermissionsStub(true)
            //                }
            //            }
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