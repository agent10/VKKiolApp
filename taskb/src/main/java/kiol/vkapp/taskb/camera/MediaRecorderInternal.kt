package kiol.vkapp.taskb.camera

import android.content.Context
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.util.Size
import android.view.Surface
import timber.log.Timber

class MediaRecorderInternal(private val context: Context) {
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
            mediaRecorder.stop()
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
        } else if(configured) {
            mediaRecorder.reset()
            configured = false
        }

        Timber.d("Start setup")

        val profile = chooseCamcorderProfile(cameraConfig)
        mediaRecorder.setOnInfoListener { mr, what, extra ->
            Timber.d("MediaRecorder mr = $mr, what = $what, $extra = $extra")
        }
        mediaRecorder.setOnErrorListener { mr, what, extra ->
            Timber.e("MediaRecorder, error: mr = $mr, what = $what, $extra = $extra")

        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setOutputFile(context.filesDir.absolutePath + "/myvideo.mp4")
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate)
        mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate)
        mediaRecorder.setAudioSamplingRate(profile.audioSampleRate)
        mediaRecorder.setVideoFrameRate(profile.videoFrameRate)
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setOrientationHint(90)
        //        val rotation = activity?.getWindowManager()?.defaultDisplay?.rotation
        //        switch(mSensorOrientation) {
        //            case SENSOR_ORIENTATION_DEFAULT_DEGREES :
        //            mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
        //            break;
        //            case SENSOR_ORIENTATION_INVERSE_DEGREES :
        //            mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
        //            break;
        //        }
        mediaRecorder.prepare()
        configured = true
        Timber.d("Finish setup")
    }

    private fun chooseCamcorderProfile(cameraConfig: CameraConfigurator.Config): CamcorderProfile {
        var quality = CamcorderProfile.QUALITY_LOW
        if (cameraConfig.mediaRecorderSize.contains(Size(1280, 720))) {
            quality = CamcorderProfile.QUALITY_720P
        } else if (cameraConfig.mediaRecorderSize.contains(Size(720, 480))) {
            quality = CamcorderProfile.QUALITY_480P
        }
        return CamcorderProfile.get(quality)
    }
}