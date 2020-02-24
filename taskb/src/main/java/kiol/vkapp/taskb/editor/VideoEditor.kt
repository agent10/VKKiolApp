package kiol.vkapp.taskb.editor

import android.content.Context
import android.graphics.Bitmap
import android.media.*
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer


class VideoEditor(app: Context, file: String, cutFile: String) {

    private val previewsExtractor = PreviewsExtractor(app, file)
    private val videoCutter = VideoCutter(file, cutFile)

    fun getThumbnails(): Flowable<Bitmap> {
        return previewsExtractor.getPreviews()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun cut(startUs: Long, endUs: Long, withAudio: Boolean) {
        videoCutter.cut(startUs, endUs, withAudio)
    }
}