package kiol.vkapp.cam.editor

import android.media.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.*
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class VideoCutter(private val file: String, private val cutFile: String) {
    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024
        private const val MinimumCutDurationUs = 800000
        private const val OffsetMinimumCutDurationUs = 150000
    }


    fun trySaveFile(fd: FileDescriptor): Completable {
        return Completable.create {

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = FileInputStream(cutFile)
                outputStream = FileOutputStream(fd)
                inputStream.copyTo(outputStream)
                it.onComplete()
            } catch (e: Exception) {
                Timber.e("Can't save cutted video: $e")
                it.onError(e)
            } finally {
                try {
                    inputStream?.close()
                    outputStream?.close()
                } catch (e: Exception) {
                }
            }
        }.subscribeOn(Schedulers.io())
    }

    fun cut(startUs: Long, endUs: Long, withAudio: Boolean) {
        val t = System.currentTimeMillis()
        val d = Flowable.fromCallable {
            genVideoUsingMuxer(file, cutFile, startUs, endUs, withAudio)
        }.subscribeOn(Schedulers.computation()).subscribe({
            Timber.d("Cut finished, time: ${System.currentTimeMillis() - t}ms")
        }, {
            Timber.e("Cut error: $it")
        })
    }

    @Throws(IOException::class)
    private fun genVideoUsingMuxer(
        srcPath: String, dstPath: String,
        startUs: Long, endUs: Long, useAudio: Boolean
    ) {
        val extractor = MediaExtractor()
        extractor.setDataSource(srcPath)
        val trackCount: Int = extractor.trackCount
        val muxer = MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val indexMap: HashMap<Int, Int> = HashMap(trackCount)
        var bufferSize = -1
        for (i in 0 until trackCount) {
            val format: MediaFormat = extractor.getTrackFormat(i)
            val mime: String = format.getString(MediaFormat.KEY_MIME)
            var selectCurrentTrack = true
            if (mime.startsWith("audio/") && !useAudio) {
                selectCurrentTrack = false
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
        val retrieverSrc = MediaMetadataRetriever()
        retrieverSrc.setDataSource(srcPath)
        val degreesString = retrieverSrc.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )
        val durationUs = retrieverSrc.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        ).toLong() * 1000L

        retrieverSrc.release()

        if (degreesString != null) {
            val degrees = degreesString.toInt()
            if (degrees >= 0) {
                muxer.setOrientationHint(degrees)
            }
        }

        var internalStartUs = startUs
        var internalEndUs = if (endUs <= 0) durationUs else endUs
        val durUs = internalEndUs - startUs
        Timber.d("Try cut with new duration $durUs us")
        if (durUs < MinimumCutDurationUs) {
            internalStartUs -= OffsetMinimumCutDurationUs
            internalStartUs = max(internalStartUs, 0)

            internalEndUs += OffsetMinimumCutDurationUs
            internalEndUs = min(internalEndUs, durationUs)
        }



        if (internalStartUs > 0) {
            extractor.seekTo(internalStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        }
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
        } catch (e: IllegalStateException) {
            Timber.e("The source video file is malformed")
        } finally {
            extractor.release()
            muxer.release()
        }
        return
    }
}