package kiol.vkapp.taskb.camera

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import kiol.vkapp.taskb.R
import kiol.vkapp.taskb.camera.components.MediaRecorderInternal
import kiol.vkapp.taskb.camera.components.Recognizer
import kiol.vkapp.taskb.camera.components.Torch
import kiol.vkapp.taskb.camera.components.Zoomer
import timber.log.Timber

class CaptureSessionCreator(
    private val context: Context,
    private val configFinished: (session: CameraCaptureSession) -> Unit
) {

    interface SessionStrategy {
        fun onPreSetup(cameraConfig: CameraConfigurator.Config)
        fun onSurfaces(surfaces: MutableList<Surface>)
        fun onSessionConfigured(previewRequestBuilder: CaptureRequest.Builder, cameraCaptureSession: CameraCaptureSession)
    }

    abstract class RecorderCameraSessionStrategy(
        private val mediaRecorderInternal: MediaRecorderInternal,
        protected val torch: Torch, private val zoomer: Zoomer, protected val recognizer: Recognizer
    ) :
        SessionStrategy {
        override fun onPreSetup(cameraConfig: CameraConfigurator.Config) {
            mediaRecorderInternal.setup(cameraConfig)
            zoomer.setConfg(cameraConfig)
        }

        override fun onSurfaces(surfaces: MutableList<Surface>) {
            surfaces += mediaRecorderInternal.getSurface()
        }

        override fun onSessionConfigured(
            previewRequestBuilder: CaptureRequest.Builder,
            cameraCaptureSession: CameraCaptureSession
        ) {
            zoomer.setup(previewRequestBuilder, cameraCaptureSession)
        }
    }

    class BackCameraSessionStrategy(
        zoomer: Zoomer,
        private val imageReader: ImageReader,
        mediaRecorderInternal: MediaRecorderInternal,
        torch: Torch, recognizer: Recognizer
    ) :
        RecorderCameraSessionStrategy
            (mediaRecorderInternal, torch, zoomer, recognizer) {

        override fun onPreSetup(cameraConfig: CameraConfigurator.Config) {
            super.onPreSetup(cameraConfig)
            recognizer.isEnabled = true
        }

        override fun onSurfaces(surfaces: MutableList<Surface>) {
            super.onSurfaces(surfaces)
            surfaces += imageReader.surface
        }

        override fun onSessionConfigured(
            previewRequestBuilder: CaptureRequest.Builder,
            cameraCaptureSession: CameraCaptureSession
        ) {
            super.onSessionConfigured(previewRequestBuilder, cameraCaptureSession)
            torch.setup(previewRequestBuilder, cameraCaptureSession)
        }
    }

    class FrontCameraSessionStrategy(
        zoomer: Zoomer,
        mediaRecorderInternal: MediaRecorderInternal,
        torch: Torch, recognizer: Recognizer
    ) :
        RecorderCameraSessionStrategy
            (mediaRecorderInternal, torch, zoomer, recognizer) {

        override fun onPreSetup(cameraConfig: CameraConfigurator.Config) {
            super.onPreSetup(cameraConfig)
            recognizer.isEnabled = false
        }

        override fun onSessionConfigured(
            previewRequestBuilder: CaptureRequest.Builder,
            cameraCaptureSession: CameraCaptureSession
        ) {
            super.onSessionConfigured(previewRequestBuilder, cameraCaptureSession)
            torch.reset()
        }
    }

    fun create(
        sessionStrategy: SessionStrategy,
        camera: CameraDevice,
        textureView: TextureView,
        cameraConfig: CameraConfigurator.Config,
        backgroundHandler: Handler
    ) {
        try {
            sessionStrategy.onPreSetup(cameraConfig)

            val texture = textureView.surfaceTexture
            texture.setDefaultBufferSize(cameraConfig.previewSize.width, cameraConfig.previewSize.height)

            val surface = Surface(texture)
            val surfaces = arrayListOf<Surface>()
            surfaces += surface

            sessionStrategy.onSurfaces(surfaces)

            val previewRequestBuilder = camera.createCaptureRequest(
                CameraDevice.TEMPLATE_RECORD
            )

            surfaces.forEach {
                previewRequestBuilder.addTarget(it)
            }

            camera.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        Timber.d("onConfigured started")
                        sessionStrategy.onSessionConfigured(previewRequestBuilder, cameraCaptureSession)
                        try {
                            previewRequestBuilder.let { builder ->
                                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                                val previewRequest = builder.build()
                                cameraCaptureSession.setRepeatingRequest(
                                    previewRequest,
                                    null, backgroundHandler
                                )
                                Timber.d("onConfigured finished")
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }

                    override fun onClosed(session: CameraCaptureSession) {
                        super.onClosed(session)
                        Timber.d("onConfigureFailed")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Timber.e("onConfigureFailed")
                    }

                    override fun onReady(session: CameraCaptureSession) {
                        super.onReady(session)
                        configFinished(session)
                        Timber.d("onReady, cid = ${session.device.id}")

                    }

                    override fun onActive(session: CameraCaptureSession) {
                        super.onActive(session)
                        Timber.d("onActive, cid = ${session.device.id}")
                    }
                }, backgroundHandler
            )
        } catch (e: Exception) {
            Timber.e(e)
            Toast.makeText(context, R.string.camera_open_failed, Toast.LENGTH_LONG).show()
        }
    }
}