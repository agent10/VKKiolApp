package kiol.vkapp.taskb.editor

import android.icu.util.TimeUnit
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
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
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.taskb.R
import timber.log.Timber

class VideoEditorFragment : Fragment(R.layout.video_editor_fragment_layout) {

    private lateinit var videoEditor: VideoEditor
    private lateinit var timebar: VideoEditorTimebar
    private var exoPlayer: SimpleExoPlayer? = null

    private var lastVolume = -1f

    private var lastStartUs = 0L
    private var lastEndUs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        videoEditor = VideoEditor(requireContext().applicationContext, requireContext().filesDir.absolutePath + "/myvideo.mp4")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerView = view.findViewById<PlayerView>(R.id.playerView)
        val updateView = view.findViewById<View>(R.id.updateView)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.soundOff -> {
                    if (lastVolume == -1f) {
                        lastVolume = exoPlayer?.volume ?: -1f
                        exoPlayer?.volume = 0f
                    } else {
                        exoPlayer?.volume = lastVolume
                        lastVolume = -1f
                    }
                }
                R.id.save -> {
                    videoEditor.cut(lastStartUs, lastEndUs, lastVolume == -1f)
                }
                else -> {
                }
            }
            true
        }

        timebar = view.findViewById(R.id.timeBar)

        val d1 = Flowable.interval(100, java.util.concurrent.TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                timebar.setPosition(exoPlayer?.currentPosition ?: 0L, exoPlayer?.duration ?: 0L)

                exoPlayer?.let {
                    if (it.duration != 0L) {
                        timebar.setPosition(it.currentPosition.toFloat() / it.duration)
                    }
                }
            }

        val d = videoEditor.getThumbnails2().subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                timebar.addThumbnail(it)
            }, {

            })

        timebar.onCutCallback = { left, right, inProcess ->
            if (inProcess) {
                exoPlayer?.stop()
            } else {
                timebar.setPosition(0.0f)
                updateView.animate().alpha(1.0f).withEndAction {
                    updateMediaSource(left, right)
                    updateView.animate().alpha(0.0f).duration = 250
                }.duration = 250
            }
        }

        val context = requireContext()
        exoPlayer = SimpleExoPlayer.Builder(context).setTrackSelector(DefaultTrackSelector(context)).build()
        playerView.player = exoPlayer
        playerView.isEnabled = false

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

    private fun updateMediaSource(left: Float = 0.0f, right: Float = 1.0f) {
        val uri = Uri.parse(requireContext().filesDir.absolutePath + "/myvideo.mp4")

        lastStartUs = ((videoEditor.duration) * left * 1000L).toLong()
        lastEndUs = ((videoEditor.duration) * right * 1000L).toLong()
        val mediaSource = ClippingMediaSource(createMediaSource(uri), lastStartUs, lastEndUs)

        mediaSource?.let {
            exoPlayer?.prepare(it)
        } ?: Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
    }
}