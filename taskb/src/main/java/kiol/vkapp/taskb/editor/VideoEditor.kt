package kiol.vkapp.taskb.editor

import android.content.Context
import android.graphics.Bitmap
import android.media.*
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer


class VideoEditor(private val app: Context, private val file: String) {
    private val mediaMetadataRetriever = MediaMetadataRetriever().apply {
        setDataSource(file)
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024
    }

    private val previewsExtractor = PreviewsExtractor(app, file)

    val duration = mediaMetadataRetriever.extractMetadata(METADATA_KEY_DURATION).toLong()

    fun getThumbnails2(): Flowable<Bitmap> {
        return previewsExtractor.getPreviews().subscribeOn(Schedulers.computation())
    }

    fun cut(startUs: Long, endUs: Long, withAudio: Boolean) {
        val t = System.currentTimeMillis()
        val d = Flowable.fromCallable {
            genVideoUsingMuxer(file, file + "cutted.mp4", startUs, endUs, withAudio, true)
        }.subscribeOn(Schedulers.computation()).subscribe({
            Timber.d("Cut finished, time: ${System.currentTimeMillis() - t}ms")
        }, {
            Timber.e("Cut error: $it")

        })
    }

    @Throws(IOException::class)
    private fun genVideoUsingMuxer(
        srcPath: String, dstPath: String,
        startUs: Long, endUs: Long, useAudio: Boolean, useVideo: Boolean
    ) { // Set up MediaExtractor to read from the source.
        val extractor = MediaExtractor()
        extractor.setDataSource(srcPath)
        val trackCount: Int = extractor.trackCount
        // Set up MediaMuxer for the destination.
        val muxer = MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Set up the tracks and retrieve the max buffer size for selected
        // tracks.
        val indexMap: HashMap<Int, Int> = HashMap(trackCount)
        var bufferSize = -1
        for (i in 0 until trackCount) {
            val format: MediaFormat = extractor.getTrackFormat(i)
            val mime: String = format.getString(MediaFormat.KEY_MIME)
            var selectCurrentTrack = false
            if (mime.startsWith("audio/") && useAudio) {
                selectCurrentTrack = true
            } else if (mime.startsWith("video/") && useVideo) {
                selectCurrentTrack = true
            }
            if (selectCurrentTrack) {
                extractor.selectTrack(i)
                val dstIndex: Int = muxer.addTrack(format)
                indexMap[i] = dstIndex
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val newSize: Int = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    bufferSize = if (newSize > bufferSize) newSize else bufferSize
                }
            }
        }
        if (bufferSize < 0) {
            bufferSize = DEFAULT_BUFFER_SIZE
        }
        // Set up the orientation and starting time for extractor.
        val retrieverSrc = MediaMetadataRetriever()
        retrieverSrc.setDataSource(srcPath)
        val degreesString = retrieverSrc.extractMetadata(
            METADATA_KEY_VIDEO_ROTATION
        )
        if (degreesString != null) {
            val degrees = degreesString.toInt()
            if (degrees >= 0) {
                muxer.setOrientationHint(degrees)
            }
        }
        if (startUs > 0) {
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_NEXT_SYNC)
        }
        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
        // for copying each sample and stop when we get to the end of the source
        // file or exceed the end time of the trimming.
        val offset = 0
        var trackIndex = -1
        val dstBuf: ByteBuffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        try {
            muxer.start()
            while (true) {
                bufferInfo.offset = offset
                bufferInfo.size = extractor.readSampleData(dstBuf, offset)
                if (bufferInfo.size < 0) {
                    Timber.e("Saw input EOS")
                    bufferInfo.size = 0
                    break
                } else {
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    val internalEndUs = if (endUs <= 0) duration else endUs

                    if (internalEndUs > 0 && bufferInfo.presentationTimeUs > internalEndUs) {
                        Timber.e("The current sample is over the trim end time.")
                        break
                    } else {
                        bufferInfo.flags = extractor.sampleFlags
                        trackIndex = extractor.sampleTrackIndex
                        muxer.writeSampleData(
                            indexMap[trackIndex]!!, dstBuf,
                            bufferInfo
                        )
                        extractor.advance()
                    }
                }
            }
            muxer.stop()
            //deleting the old file
            //            val file = File(srcPath)
            //            file.delete()
        } catch (e: IllegalStateException) { // Swallow the exception due to malformed source.
            Timber.e("The source video file is malformed")
        } finally {
            muxer.release()
        }
        return
    }
}