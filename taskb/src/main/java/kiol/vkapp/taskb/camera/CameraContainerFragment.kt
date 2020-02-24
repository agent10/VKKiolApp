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
import kotlinx.android.synthetic.main.camera_container_fragment.*
import timber.log.Timber

class CameraContainerFragment : Fragment(R.layout.camera_container_fragment) {

    companion object {
        private const val PermissionRequestCode = 42
    }

    private lateinit var myCamera: MyCamera
    private lateinit var torch: CheckableImageButton
    private lateinit var camSwithcProgress: ProgressBar
    private lateinit var camSwitcher: ImageButton

    private var torchX = 0f
    private var switchX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myCamera = MyCamera(requireContext())
    }

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

        camSwithcProgress = view.findViewById(R.id.camSwitchProgress)

        camSwitcher = view.findViewById(R.id.camSwitcher)
        camSwitcher.setOnClickListener {
            if (!myCamera.isCamHardWorking) {
                changeCamSwithcProgress(true).withEndAction {
                    myCamera.switchCamera()
                }
            }
        }

        torch = view.findViewById(R.id.torchSwitcher)
        torch.setOnClickListener {
            myCamera.setTorch(!myCamera.isTorchEnabled())
        }

        val recordBtn = view.findViewById<RecordButton>(R.id.recordBtn)
        recordBtn.callback = object : RecordButton.Callback {
            override fun onZoomLevel(zoomLevel: Float) {
                myCamera.setZoom(zoomLevel)
            }

            override fun onRecord(started: Boolean) {
                if (started) {
                    myCamera.startRecord()
                    changeCamSwitchButton(false)
                    changeTorchButton(false)
                } else {
                    myCamera.stopRecord()
                    changeCamSwitchButton(true)
                    changeTorchButton(true)
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

        myCamera.onCamRecord = {
            if (it is MyCamera.Record.End) {
                if (it.timeMs >= MIN_VALID_RECORD_TIME_MS) {
                    getSimpleRouter().routeToEditor()
                } else {
                    Toast.makeText(requireContext(), "The video is too short", Toast.LENGTH_SHORT).show()
                }
            }
        }

        myCamera.camSwitchFinished = {
            changeCamSwithcProgress(false)
            when (it) {
                Back -> changeTorchButton(true)
                Front -> changeTorchButton(false)
            }
            torch.isChecked = myCamera.isTorchEnabled()
        }

        val d = myCamera.getQrListener().subscribe({
            setEnableQrCallback(false)
            QrDialog.create(it).show(childFragmentManager, null)
        }, {
            Timber.e(it)
        })
    }

    fun setEnableQrCallback(value: Boolean) {
        myCamera.setEnableQrCallback(value)
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