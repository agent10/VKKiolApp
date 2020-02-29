package kiol.vkapp.cam.camera.components

import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.util.Size
import android.view.Surface
import kiol.vkapp.cam.camera.CameraConfigurator
import kiol.vkapp.cam.camera.MyCamera
import timber.log.Timber

class MediaRecorderInternal(private val file: String) {
    class MediaRecorderNotConfigured : Exception()

    private val mediaRecorder = MediaRecorder()

    private var configured = false
    var isRecording = false

    fun record() {
        if (configured && !isRecording) {
            mediaRecorder.start()
            Timber.d("Start recorder")
            isRecording = true
        }
    }

    fun stop() {
        if (configured && isRecording) {
            configured = false
            try {
                mediaRecorder.stop()
            } catch (e: Exception) {
                Timber.e("Error during recording stop, e: $e")
            }
            mediaRecorder.reset()
            Timber.d("Stop and reset recorder")
            isRecording = false
        }
    }

    fun getSurface(): Surface {
        if (configured) {
            return mediaRecorder.surface
        }
        throw MediaRecorderNotConfigured()
    }

    fun setup(cameraConfig: CameraConfigurator.Config) {
        if (isRecording) {
            stop()
        } else if (configured) {
            mediaRecorder.reset()
            configured = false
        }

        val profile = chooseCamcorderProfile(cameraConfig)
        mediaRecorder.setOnInfoListener { mr, what, extra ->
            Timber.d("MediaRecorder mr = $mr, what = $what, $extra = $extra")
        }
        mediaRecorder.setOnErrorListener { mr, what, extra ->
            Timber.e("MediaRecorder, error: mr = $mr, what = $what, $extra = $extra")

        }
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setOutputFile(file)
            mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate)
            mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate)
            mediaRecorder.setAudioSamplingRate(profile.audioSampleRate)
            mediaRecorder.setVideoFrameRate(profile.videoFrameRate)
            mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setOrientationHint(if (cameraConfig.isFaceCamera) 270 else 90)
            mediaRecorder.prepare()
            configured = true
        } catch (e: Exception) {
            throw MyCamera.CameraFatalException("MediaRecorder config failed")
        }
    }

    private fun chooseCamcorderProfile(cameraConfig: CameraConfigurator.Config): CamcorderProfile {
        val camId = cameraConfig.cameraId.toInt()
        var quality = CamcorderProfile.QUALITY_LOW
        if (cameraConfig.mediaRecorderSize.contains(Size(1280, 720))) {
            quality = CamcorderProfile.QUALITY_720P
        } else if (cameraConfig.mediaRecorderSize.contains(Size(720, 480))) {
            quality = CamcorderProfile.QUALITY_480P
        }
        return try {
            CamcorderProfile.get(camId, quality)
        } catch (e: Exception) {
            Timber.e("Can't set CamcorderProfile, fallback to lowest quality")
            CamcorderProfile.get(camId, CamcorderProfile.QUALITY_LOW)
        }
    }
}