package kiol.vkapp.taskb.editor

import android.icu.util.TimeUnit
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ClippingMediaPeriod
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.R
import timber.log.Timber

class VideoEditorFragment : Fragment(R.layout.video_editor_fragment_layout) {

    private lateinit var videoEditor: VideoEditor
    private var exoPlayer: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoEditor = VideoEditor(requireContext().filesDir.absolutePath + "/myvideo.mp4")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerView = view.findViewById<PlayerView>(R.id.playerView)
        val tempTh = view.findViewById<ImageView>(R.id.tempThumb)

        val d = videoEditor.getThumbnails().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            tempTh.setImageBitmap(it.second)
        }, {

        })

        videoEditor.cut(3000, 13000)

        val context = requireContext()
        exoPlayer = SimpleExoPlayer.Builder(context).setTrackSelector(DefaultTrackSelector(context)).build()
        playerView.player = exoPlayer

        setupPlayer()
    }

    private fun setupPlayer() {
        exoPlayer?.playWhenReady = true
        exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL

        updateMediaSource()
    }

    private fun createMediaSource(uri: Uri): MediaSource? {
        val type = Util.inferContentType(uri)
        return when (@ContentType type) {
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(FileDataSource.Factory()).createMediaSource(uri)
            else -> {
                Timber.e("Unknown URI format, can't create MediaSource")
                null
            }
        }
    }

    private fun updateMediaSource() {
        val uri = Uri.parse(requireContext().filesDir.absolutePath + "/myvideo.mp4")

        val mediaSource = ClippingMediaSource(createMediaSource(uri), 0, C.TIME_END_OF_SOURCE)

        mediaSource?.let {
            exoPlayer?.prepare(it)
        } ?: Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
    }
}