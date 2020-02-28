package kiol.vkapp.taskb.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import kiol.vkapp.taskb.R
import kiol.vkapp.taskb.camera.MyCamera.CameraType.*
import kiol.vkapp.taskb.camera.MyCamera.Companion.MIN_VALID_RECORD_TIME_MS
import kiol.vkapp.taskb.camera.ui.CheckableImageButton
import kiol.vkapp.taskb.camera.ui.QrDialog
import kiol.vkapp.taskb.camera.ui.QrOverlay
import kiol.vkapp.taskb.camera.ui.RecordButton
import kiol.vkapp.taskb.getSimpleRouter
import kiol.vkapp.taskb.getTempVideoFile
import kotlinx.android.synthetic.main.camera_container_fragment.*
import timber.log.Timber

class CameraContainerFragment : Fragment(R.layout.camera_container_fragment) {

    companion object {
        private const val PermissionRequestCode = 42
    }

    private lateinit var myCamerasManager: MyCamerasManager
    private var currentCamera: MyCamera? = null

    private lateinit var torch: CheckableImageButton
    private lateinit var camSwithcProgress: ProgressBar
    private lateinit var camSwitcher: ImageButton

    private var torchX = 0f
    private var switchX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        myCamerasManager = MyCamerasManager(requireContext(), getTempVideoFile())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")

        view.findViewById<TextureView>(R.id.cameraView).apply {
            myCamerasManager.setTextureView(this)
        }

        view.findViewById<QrOverlay>(R.id.qrOverlay).apply {
            //            myCamera.setQROverlay(this)
        }

        camSwithcProgress = view.findViewById(R.id.camSwitchProgress)

        camSwitcher = view.findViewById(R.id.camSwitcher)
        camSwitcher.setOnClickListener {
            changeCamSwithcProgress(true).withEndAction {
                myCamerasManager.switchCameraFace()
            }
        }

        torch = view.findViewById(R.id.torchSwitcher)
        torch.setOnClickListener {
            myCamerasManager.switchTorch()
        }

        val recordBtn = view.findViewById<RecordButton>(R.id.recordBtn)
        recordBtn.callback = object : RecordButton.Callback {
            override fun onZoomLevel(zoomLevel: Float) {
                myCamerasManager.setZoom(zoomLevel)
            }

            override fun onRecord(started: Boolean) {
                if (started) {
                    myCamerasManager.startRecord()

                    changeCamSwitchButton(false)
                    changeTorchButton(false)
                } else {
                    Timber.d("rectest1")

                    myCamerasManager.stopRecord()

                    changeCamSwitchButton(true)
                    if (myCamerasManager.getCurrentCameraType() == Back) {
                        changeTorchButton(true)
                    }
                }
            }
        }

        torch.doOnLayout {
            torchX = it.x
        }
        camSwitcher.doOnLayout {
            switchX = it.x
        }

        cameraView.doOnLayout {
            recordBtn.zoomHeight = it.measuredHeight.toFloat()
        }

        myCamerasManager.onCameraChanged = {
            currentCamera = it
        }

        myCamerasManager.onCameraTypeChanged = {
            when (it) {
                Back -> changeTorchButton(true)
                Front -> changeTorchButton(false)
            }
        }

        myCamerasManager.onCameraSwitchingFinished = {
            changeCamSwithcProgress(false)
        }

        myCamerasManager.onCamRecordEnd = {
            getSimpleRouter().routeToEditor()
        }

        //        val d = myCamera.getQrListener().subscribe({
        //            setEnableQrCallback(false)
        //            QrDialog.create(it).show(childFragmentManager, null)
        //        }, {
        //            Timber.e(it)
        //        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("onDestroyView")
    }

    fun setEnableQrCallback(value: Boolean) {
        //        myCamera.setEnableQrCallback(value)
    }

    private fun changeTorchButton(show: Boolean): ViewPropertyAnimator {
        torch.clearAnimation()
        return if (show) {
            torch.animate().alpha(1.0f).x(torchX).apply { duration = 100 }
        } else {
            torch.animate().alpha(0.0f).translationXBy(-20f).apply { duration = 100 }
        }
    }

    private fun changeCamSwithcProgress(show: Boolean): ViewPropertyAnimator {
        return if (show) {
            camSwithcProgress.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).apply {
                duration = 100
            }
        } else {
            camSwithcProgress.animate().alpha(0.0f).scaleX(0.5f).scaleY(0.5f).apply { duration = 100 }
        }
    }

    private fun changeCamSwitchButton(show: Boolean): ViewPropertyAnimator {
        camSwitcher.clearAnimation()
        return if (show) {
            camSwitcher.animate().alpha(1.0f).x(switchX).apply { duration = 100 }

        } else {
            camSwitcher.animate().alpha(0.0f).translationXBy(20f).apply { duration = 100 }
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart")
        if (isCameraPermissionGranted()) {
            myCamerasManager.createCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), PermissionRequestCode)
        }
    }

    override fun onStop() {
        myCamerasManager.stopCamera()
        super.onStop()
        Timber.d("onStop")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == PermissionRequestCode) {
            Timber.d("perms, onRequestPermissionsResult, grantResults: $grantResults, permissions: $permissions")
            if (grantResults.all { it == 1 }) {
                myCamerasManager.createCamera()
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