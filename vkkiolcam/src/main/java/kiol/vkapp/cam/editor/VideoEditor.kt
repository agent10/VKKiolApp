package kiol.vkapp.cam.editor

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.FileDescriptor


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

    fun saveCuttedFile(fd: FileDescriptor) = videoCutter.trySaveFile(fd).observeOn(AndroidSchedulers.mainThread())
}