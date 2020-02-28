package kiol.vkapp.taskb.camera

import android.content.Context
import android.view.TextureView

class MyCamerasManager(private val context: Context, private val tempFile: String) {

    private var currentCamera: MyCamera? = null
    private var nextCamera: MyCamera? = null

    private lateinit var textureView: TextureView

    private var isSwitching = false
    private var lastSwitchCamera = MyCamera.CameraType.Back

    var onCameraChanged: (MyCamera) -> Unit = {}
    var onCameraSwitchingFinished: () -> Unit = {}
    var onCameraTypeChanged: (MyCamera.CameraType) -> Unit = {}
    var onCurrentCameraStateChanged: (MyCamera, MyCamera.CameraState) -> Unit = { _, _ -> }

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

    private fun setNewCam(camera: MyCamera) {
        currentCamera = camera
        onCameraChanged(camera)
        onCameraTypeChanged(camera.cameraType)

        currentCamera?.apply {
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
                    handleCurrentFinishedState()
                }
            }

            startCamera()
        }
    }

    private fun handleCurrentFinishedState() {
        currentCamera = null

        nextCamera?.let {
            setNewCam(it)
            nextCamera = null
        }
    }

    fun stopCamera() {
        nextCamera = null

        currentCamera?.stopCamera()
    }
}