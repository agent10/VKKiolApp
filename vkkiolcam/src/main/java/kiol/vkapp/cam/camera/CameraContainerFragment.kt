package kiol.vkapp.cam.camera

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import kiol.vkapp.commonui.permissions.PermissionManager
import kiol.vkapp.commonui.pxF
import kiol.vkapp.cam.R
import kiol.vkapp.cam.camera.MyCamera.CameraType.Back
import kiol.vkapp.cam.camera.MyCamera.CameraType.Front
import kiol.vkapp.cam.camera.ui.CheckableImageButton
import kiol.vkapp.cam.camera.ui.QrDialog
import kiol.vkapp.cam.camera.ui.QrOverlay
import kiol.vkapp.cam.camera.ui.RecordButton
import kiol.vkapp.cam.getSimpleRouter
import kiol.vkapp.cam.getTempVideoFile
import kotlinx.android.synthetic.main.camera_container_fragment.*
import timber.log.Timber


class CameraContainerFragment : Fragment(R.layout.camera_container_fragment) {

    companion object {
        private const val AnimDuration = 100L
    }

    private lateinit var myCamerasManager: MyCamerasManager
    private var currentCamera: MyCamera? = null

    private lateinit var torch: CheckableImageButton
    private lateinit var camSwithcProgress: ProgressBar
    private lateinit var camSwitcher: ImageButton

    private lateinit var noPermissionsLayout: ViewGroup

    private var torchX = 0f
    private var switchX = 0f

    private val iconsOffset = 6.pxF

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        permissionManager = PermissionManager(
            requireContext(), listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
        myCamerasManager = MyCamerasManager(requireContext(), getTempVideoFile())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")

        view.findViewById<ViewGroup>(R.id.rootCameraLayout).doOnLayout {
        }

        noPermissionsLayout = view.findViewById(R.id.rootNoPermissionsLayout)

        view.findViewById<TextView>(R.id.noPermsDescription).apply {
            setText(R.string.permission_request)
        }

        view.findViewById<TextureView>(R.id.cameraView).apply {
            myCamerasManager.setTextureView(this)
        }

        view.findViewById<QrOverlay>(R.id.qrOverlay).apply {
            myCamerasManager.setQrOverlay(this)
        }

        camSwithcProgress = view.findViewById(R.id.camSwitchProgress)

        camSwitcher = view.findViewById(R.id.camSwitcher)
        camSwitcher.setOnClickListener {
            camSwithcProgress.visibility = View.VISIBLE
            changeCamSwithcProgress(true).withEndAction {
                myCamerasManager.switchCameraFace()
            }
        }

        torch = view.findViewById(R.id.torchSwitcher)
        torch.setOnClickListener {
            myCamerasManager.switchTorch()
        }
        torch.alpha = if (myCamerasManager.isBackLastCamType()) 1.0f else 0.0f

        val recordBtn = view.findViewById<RecordButton>(R.id.recordBtn)
        recordBtn.callback = object : RecordButton.Callback {
            override fun onZoomLevel(zoomLevel: Float) {
                myCamerasManager.setZoom(zoomLevel)
            }

            override fun onRecord(started: Boolean) {
                if (started) {
                    setEnableQrCallback(false)
                    myCamerasManager.startRecord()

                    changeCamSwitchButton(false)
                    changeTorchButton(false)
                } else {
                    setEnableQrCallback(true)
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
            if (torch.isLaidOut) {
                when (it) {
                    Back -> changeTorchButton(true)
                    Front -> changeTorchButton(false)
                }
            }
        }

        myCamerasManager.onCameraSwitchingFinished = {
            changeCamSwithcProgress(false).withEndAction {
                camSwithcProgress.visibility = View.INVISIBLE

                when (myCamerasManager.getCurrentCameraType()) {
                    Back -> changeTorchButton(true)
                    Front -> changeTorchButton(false)
                }
            }
        }

        myCamerasManager.onCamRecordEnd = {
            getSimpleRouter().routeToEditor()
        }

        myCamerasManager.onQrReceived = {
            QrDialog.create(it).show(childFragmentManager, null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("onDestroyView")
    }

    fun setEnableQrCallback(value: Boolean) {
        myCamerasManager.setEnableQrCallback(value)
    }

    private fun changeTorchButton(show: Boolean): ViewPropertyAnimator {
        torch.clearAnimation()
        return if (show) {
            torch.animate().alpha(1.0f).x(torchX).apply { duration = AnimDuration }
        } else {
            torch.animate().alpha(0.0f).translationXBy(-iconsOffset).apply { duration = AnimDuration }
        }
    }

    private fun changeCamSwithcProgress(show: Boolean): ViewPropertyAnimator {
        return if (show) {
            camSwithcProgress.animate().alpha(1.0f)
                .scaleX(1.0f).scaleY(1.0f).apply { duration = AnimDuration }
        } else {
            camSwithcProgress.animate().alpha(0.0f)
                .scaleX(0.5f).scaleY(0.5f).apply { duration = AnimDuration }
        }
    }

    private fun changeCamSwitchButton(show: Boolean): ViewPropertyAnimator {
        camSwitcher.clearAnimation()
        return if (show) {
            camSwitcher.animate().alpha(1.0f).x(switchX).apply { duration = AnimDuration }

        } else {
            camSwitcher.animate().alpha(0.0f).translationXBy(iconsOffset).apply { duration = AnimDuration }
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart")
        permissionManager.checkPermissions(this) { granted ->
            noPermissionsLayout.visibility = if (granted) View.GONE else View.VISIBLE
            if (granted) {
                myCamerasManager.createCamera()
            }
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
        permissionManager.invokePermissionRequest(requestCode, permissions, grantResults)
    }
}