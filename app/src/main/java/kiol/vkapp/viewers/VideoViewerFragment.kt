package kiol.vkapp.viewers

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kiol.vkapp.R
import kiol.vkapp.ViewerNotAvailable
import kiol.vkapp.commondata.domain.DocItem
import timber.log.Timber

class VideoViewerFragment : Fragment(R.layout.video_viewer_fragment_layout) {
    companion object {
        private val supportedFormats = listOf("mkv", "mp4", "ogg", "mov", "wav", "mp3", "m4a", "m4v", "ts")
        const val URL = "doc_url"
        fun create(docItem: DocItem): VideoViewerFragment {
            if (!supportedFormats.contains(docItem.type.ext.toLowerCase())) {
                throw ViewerNotAvailable(docItem)
            }
            return VideoViewerFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, docItem.contentUrl)
                }
            }
        }
    }

    private var exoPlayer: SimpleExoPlayer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerView = view.findViewById<PlayerView>(R.id.playerView)

        val context = requireContext()
        exoPlayer = SimpleExoPlayer.Builder(context).setTrackSelector(DefaultTrackSelector(context)).build()
        playerView.player = exoPlayer

        setupPlayer()
    }

    private fun setupPlayer() {
        exoPlayer?.playWhenReady = true

        val uri = Uri.parse(arguments!!.getString(URL))

        val mediaSource = createMediaSource(uri)

        mediaSource?.let {
            exoPlayer?.prepare(it)
        } ?: Toast.makeText(requireContext(), "Can't create MediaSource", Toast.LENGTH_SHORT).show()
    }

    private fun createMediaSource(uri: Uri): MediaSource? {
        val defaultHttpDataSourceFactory = DefaultHttpDataSourceFactory("defUserAgent")

        val type = Util.inferContentType(uri)
        return when (@ContentType type) {
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(uri)
            else -> {
                Timber.e("Unknown URI format, can't create MediaSource")
                null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}