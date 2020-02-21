package kiol.vkapp.taskb.editor

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
        private const val DEQUEUE_TIMEOUT_US = 10000L
        const val NUM_FRAMES = 10
    }

    private val imageConverter = ImageConverter(context)

    fun getPreviews(): Flowable<Bitmap> {
        return Flowable.create({ emitter ->
            val extractor = setupExtractor()
            val decoder = setupDecoder(extractor)
            val mediaMetadataRetriever = setupMetadataRetriever()

            try {
                val durationUs = mediaMetadataRetriever.getDurationUs()

                val seekUsStep = durationUs / NUM_FRAMES

                var currSeek = 0L
                decoder.start()

                repeat(NUM_FRAMES) {

                    extractor.seekTo(currSeek, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    var retb: Bitmap? = null

                    val inIndex = decoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)
                        buffer?.let {
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }

                        val info: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
                        while (true) {
                            val outIndex = decoder.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                            if (outIndex >= 0) {
                                try {
                                    val outputFormat = decoder.getOutputFormat(outIndex)
                                    val rotation = outputFormat.getInteger(MediaFormat.KEY_ROTATION)

                                    val image = decoder.getOutputImage(outIndex)
                                    image?.let {
                                        retb = imageConverter.convert(it, rotation)
                                        it.close()
                                    }
                                    decoder.releaseOutputBuffer(outIndex, false)

                                } catch (e: Exception) {
                                    Timber.e("getOutputImage error: $e")
                                }
                                break
                            }
                        }
                    }

                    retb?.let {
                        emitter.onNext(it)
                    }
                    currSeek += seekUsStep
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
}