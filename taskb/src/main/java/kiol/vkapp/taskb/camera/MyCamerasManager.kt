package kiol.vkapp.taskb.camera

import android.content.Context
import android.view.TextureView
import android.widget.Toast
import kiol.vkapp.taskb.camera.MyCamera.Companion.MIN_VALID_RECORD_TIME_MS
import timber.log.Timber

class MyCamerasManager(private val context: Context, private val tempFile: String) {

    private var currentCamera: MyCamera? = null
    private var nextCamera: MyCamera? = null

    private lateinit var textureView: TextureView

    private var isSwitching = false
    private var lastSwitchCamera = MyCamera.CameraType.Back

    var onCameraChanged: (MyCamera?) -> Unit = {}
    var onCameraSwitchingFinished: () -> Unit = {}
    var onCameraTypeChanged: (MyCamera.CameraType) -> Unit = {}
    var onCurrentCameraStateChanged: (MyCamera, MyCamera.CameraState) -> Unit = { _, _ -> }

    var onCamRecordEnd: () -> Unit = { }


    fun setTextureView(textureView: TextureView) {
        this.textureView = textureView
    }

    fun createCamera() {
        createCamera(lastSwitchCamera)
    }

    private fun createCamera(cameraType: MyCamera.CameraType = MyCamera.CameraType.Back) {
        val nCamera = MyCamera(context, tempFile)
        nCamera.cameraType = cameraType
        nCamera.setTextureView(textureView)

        if (currentCamera == null) {
            setNewCam(nCamera)
        } else {
            nextCamera = nCamera
            currentCamera?.stopCamera()
        }
    }

    fun switchCameraFace() {
        val currType = currentCamera?.cameraType ?: MyCamera.CameraType.Back
        val nextType = if (currType == MyCamera.CameraType.Front)
            MyCamera.CameraType.Back
        else
            MyCamera.CameraType.Front

        isSwitching = true
        lastSwitchCamera = nextType
        createCamera(nextType)
    }

    fun getCurrentCameraType() = currentCamera?.cameraType ?: MyCamera.CameraType.Back

    fun switchTorch(): Boolean {
        return currentCamera?.switchTorch() ?: false
    }

    fun isTorchEnabled(): Boolean {
        return currentCamera?.isTorchEnabled() ?: false
    }

    fun setZoom(zoomLevel: Float) {
        currentCamera?.setZoom(zoomLevel)
    }

    private fun setNewCam(camera: MyCamera) {
        currentCamera = camera
        onCameraChanged(camera)
        onCameraTypeChanged(camera.cameraType)

        currentCamera?.apply {
            onCamRecord = {
                if (it is MyCamera.Record.End) {
                    if (it.timeMs >= MIN_VALID_RECORD_TIME_MS) {
                        this@MyCamerasManager.onCamRecordEnd()

                    } else {
                        Toast.makeText(context, "The video is too short", Toast.LENGTH_SHORT).show()
                        createCamera()
                    }
                }
            }

            onCamStateChanged = {
                onCurrentCameraStateChanged(this, it)
                if (it == MyCamera.CameraState.Ready) {
                    if (isSwitching) {
                        onCameraSwitchingFinished()
                    }
                    isSwitching = false
                }

                if (it == MyCamera.CameraState.Closed || it == MyCamera.CameraState.Error) {
                    currentCamera?.onCamStateChanged = {}
                    handleCurrentFinishedState(it == MyCamera.CameraState.Error)
                }
            }

            startCamera()
        }
    }

    fun startRecord() {
        currentCamera?.startRecord()
    }

    fun stopRecord() {
        currentCamera?.stopRecord()
    }

    private fun handleCurrentFinishedState(isError: Boolean) {
        currentCamera = null
        onCameraChanged(null)

        if (nextCamera != null) {
            nextCamera?.let {
                setNewCam(it)
            }
            nextCamera = null
        } else {
            if (isError) {
                createCamera()
            }
        }
    }

    fun stopCamera() {
        nextCamera = null

        currentCamera?.stopCamera()
    }
}