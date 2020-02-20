package kiol.vkapp.taskb.editor

import android.graphics.Bitmap
import android.media.*
import android.media.MediaMetadataRetriever.*
import android.os.Build
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer


class VideoEditor(private val file: String) {
    private val mediaMetadataRetriever = MediaMetadataRetriever().apply {
        setDataSource(file)
    }

    companion object {
        private const val NUM_FRAMES = 10
        private const val DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024
    }

    private val frameWidth = mediaMetadataRetriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH).toInt()
    private val frameHeight = mediaMetadataRetriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT).toInt()
    val duration = mediaMetadataRetriever.extractMetadata(METADATA_KEY_DURATION).toLong()

    fun getThumbnails(): Flowable<Pair<Int, Bitmap>> {
        return Flowable.create<Pair<Int, Bitmap>>({ emitter ->
            repeat(NUM_FRAMES) {
                if (Build.VERSION.SDK_INT >= 27) {
                    val b = mediaMetadataRetriever.getScaledFrameAtTime(
                        duration / NUM_FRAMES * it * 1000L, OPTION_CLOSEST,
                        frameWidth / 10, frameHeight / 10
                    )
                    emitter.onNext(Pair(0, b))
                } else {
                    val b = mediaMetadataRetriever.getFrameAtTime(duration / NUM_FRAMES * it * 1000L, OPTION_CLOSEST)
                    emitter.onNext(Pair(it, Bitmap.createScaledBitmap(b, b.width / 10, b.height / 10, true)))
                }
            }
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
    }

    fun cut(startMs: Int, endMs: Int) {
        val t = System.currentTimeMillis()
        val d = Flowable.fromCallable {
            genVideoUsingMuxer(file, file + "cutted.mp4", startMs, endMs, true, true)
        }.subscribeOn(Schedulers.computation()).subscribe({
            Timber.d("Cut finished, time: ${System.currentTimeMillis() - t}ms")
        }, {
            Timber.e("Cut error: $it")

        })
    }

    @Throws(IOException::class)
    private fun genVideoUsingMuxer(
        srcPath: String, dstPath: String,
        startMs: Int, endMs: Int, useAudio: Boolean, useVideo: Boolean
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
        if (startMs > 0) {
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_NEXT_SYNC)
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
                    if (endMs > 0 && bufferInfo.presentationTimeUs > endMs * 1000) {
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