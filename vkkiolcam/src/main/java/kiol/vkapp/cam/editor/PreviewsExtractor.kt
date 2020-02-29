package kiol.vkapp.cam.editor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import timber.log.Timber

class PreviewsExtractor(private val context: Context, private val filePath: String) {

    class DecoderInitFailed : Exception("Fail to create MediaCodec")

    companion object {
        private const val DEQUEUE_TIMEOUT_US = 0L
        const val NUM_FRAMES = 10
    }

    private val imageConverter = ImageConverter(context)

    fun getPreviews(): Flowable<Bitmap> {
        return Flowable.create({ emitter ->
            val extractor = setupExtractor()
            val decoder = setupDecoder(extractor)
            val mediaMetadataRetriever = setupMetadataRetriever()

            try {
                val rotation = mediaMetadataRetriever.getRotation()
                val durationUs = mediaMetadataRetriever.getDurationUs()

                val seekUsStep = durationUs / NUM_FRAMES

                var currSeek = 0L
                decoder.start()

                val info: MediaCodec.BufferInfo = MediaCodec.BufferInfo()

                var stop = false
                var processed = 0
                while (!stop) {
                    while (true) {
                        val inIndex = decoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                        if (inIndex >= 0) {
                            val buffer = decoder.getInputBuffer(inIndex)
                            buffer?.let {
                                buffer.clear()
                                extractor.seekTo(currSeek, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                                val size = extractor.readSampleData(buffer, 0)
                                if (size < 0) {
                                    decoder.queueInputBuffer(
                                        inIndex,
                                        0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    stop = true
                                } else {
                                    Timber.d("queueInput index: $inIndex")
                                    processed++
                                    decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                }

                                currSeek += seekUsStep
                                if (currSeek > durationUs) {
                                    Timber.d("setFlagStop")
                                    stop = true
                                }
                            }
                        } else {
                            break
                        }

                        if (stop) {
                            break
                        }
                    }

                    while (true) {
                        val outIndex = decoder.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                        if (outIndex >= 0) {
                            Timber.d("dequeueOutputBuffer index: $outIndex")

                            try {
                                val image = decoder.getOutputImage(outIndex)
                                image?.let {
                                    processed--
                                    val retb = imageConverter.convert(it, rotation)
                                    it.close()

                                    emitter.onNext(retb)
                                }
                                decoder.releaseOutputBuffer(outIndex, false)

                            } catch (e: Exception) {
                                Timber.e("getOutputImage error: $e")
                            }
                        } else {
                            if (stop) {
                                Timber.d("dequeueOutputBuffer try to stop, but wait other $processed will process")
                                if (processed == 0) {
                                    break
                                }
                            } else {
                                break
                            }
                        }
                    }

                }

                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            } finally {
                mediaMetadataRetriever.release()
                decoder.stop()
                decoder.release()
                extractor.release()
            }
        }, BackpressureStrategy.BUFFER)
    }

    private fun setupMetadataRetriever(): MediaMetadataRetriever {
        return MediaMetadataRetriever().apply {
            setDataSource(filePath)
        }
    }

    private fun setupExtractor(): MediaExtractor {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)
        return extractor
    }


    private fun setupDecoder(extractor: MediaExtractor): MediaCodec {
        val trackCount: Int = extractor.trackCount
        for (i in 0 until trackCount) {
            val format: MediaFormat = extractor.getTrackFormat(i)
            val mime: String = format.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("video/")) {
                extractor.selectTrack(i)

                val decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(format, /*surface*/ null, null, 0)
                return decoder
            }
        }

        throw DecoderInitFailed()
    }

    private fun MediaMetadataRetriever.getDurationUs(): Long {
        return extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong() * 1000L
    }

    private fun MediaMetadataRetriever.getRotation(): Int {
        return extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
    }
}