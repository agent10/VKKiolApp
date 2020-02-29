package kiol.vkapp.cam.editor

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.util.Util
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kiol.vkapp.commonui.plusAssign
import kiol.vkapp.commonui.pxF
import kiol.vkapp.cam.*
import kiol.vkapp.cam.editor.VideoEditorTimebar.SelectedCutThumb.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class VideoEditorFragment : Fragment(R.layout.video_editor_fragment_layout) {

    private lateinit var videoEditor: VideoEditor
    private var exoPlayer: SimpleExoPlayer? = null

    private var lastVolume = -1f

    private var lastStartUs = 0L
    private var lastEndUs = 0L

    private var lastSeekMs = -1

    private var initialDuration = -1L

    private var originalMediaSource: MediaSource? = null

    private lateinit var timebar: VideoEditorTimebar
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var updateView: View
    private lateinit var toolbar: Toolbar

    private val compositeDisposable = CompositeDisposable()

    private val handler = Handler()

    private var saveOnDestroy = false
    private var playOnAppResume = true

    companion object {
        private const val WAIT_FOR_MEDIARECORDER = 1000L
        private const val VIDEO_FILE_NAME_TO_SAVE = "vktaskbmyvideo.mp4"
        private const val FILE_EXPORT_REQUEST_CODE = 43
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        playerView = view.findViewById(R.id.playerView)
        updateView = view.findViewById(R.id.updateView)
        toolbar = view.findViewById(R.id.toolbar)
        timebar = view.findViewById(R.id.timeBar)


        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.soundOff -> {
                    if (lastVolume == -1f) {
                        lastVolume = exoPlayer?.volume ?: -1f
                        exoPlayer?.volume = 0f
                        it.setIcon(R.drawable.ic_sound_on)
                    } else {
                        exoPlayer?.volume = lastVolume
                        lastVolume = -1f
                        it.setIcon(R.drawable.ic_sound_off)
                    }
                }
                R.id.save -> {
                    selectFileToSave()
                }
                else -> {
                }
            }
            true
        }

        timebar.onCutCallback = { left, right, inProcess, activeThumb ->
            if (inProcess) {
                exoPlayer?.playWhenReady = false
                val roundPos = when (activeThumb) {
                    LEFT -> ((exoPlayer?.duration ?: 0L) * left / 100f).roundToLong()
                    RIGHT -> ((exoPlayer?.duration ?: 0L) * right / 100f).roundToLong()
                    NONE -> 0L
                }
                exoPlayer?.seekTo(roundPos * 100L)
            } else {
                lastSeekMs = -1
                timebar.setPosition(0.0f)
                exoPlayer?.seekTo(0)
                updateView.animate().alpha(1.0f).withEndAction {
                    updateMediaSource(left, right)
                    updateView.animate().alpha(0.0f).duration = 250
                }.duration = 250
            }
        }

        progressBar.visibility = View.VISIBLE

        handler.postDelayed({
            progressBar.visibility = View.GONE
            createVideoEditor()
            getThumbnails()
            setupPlayer()
            showUIAnimated()
        }, WAIT_FOR_MEDIARECORDER)
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        if (playOnAppResume) {
            exoPlayer?.playWhenReady = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        compositeDisposable.clear()
    }

    override fun onStop() {
        super.onStop()

        if (saveOnDestroy) {
            videoEditor.cut(lastStartUs, lastEndUs, lastVolume == -1f)
            saveOnDestroy = false
        }
    }

    private fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun createVideoEditor() {
        videoEditor = VideoEditor(getAppContext(), getTempVideoFile(), getTempCutVideoFile())
    }

    private fun getThumbnails() {
        compositeDisposable += videoEditor.getThumbnails().subscribe({ timebar.addThumbnail(it) }, {
            Timber.e("Can't load thumbnails: $it")
        })
    }

    private fun showUIAnimated() {
        toolbar.animate().alpha(1.0f).translationY(10.pxF).duration = 1000
        timebar.animate().alpha(1.0f).translationY((-10).pxF).duration = 1000
    }

    private fun setupPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        playerView.player = exoPlayer
        playerView.isEnabled = false
        exoPlayer?.playWhenReady = true
        exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer?.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                Timber.e("Playback error: $error")
                Toast.makeText(requireContext(), R.string.internal_error, Toast.LENGTH_SHORT).show()
            }
        })

        compositeDisposable += Flowable.interval(50, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                exoPlayer?.let {
                    if (it.duration > 0L) {
                        if (initialDuration == -1L) {
                            initialDuration = exoPlayer?.duration ?: -1L
                        }
                        timebar.setPosition(it.currentPosition.toFloat() / it.duration)
                    }
                }
            }

        getSimpleRouter()

        val uri = Uri.parse(getTempVideoFile())
        originalMediaSource = createMediaSource(uri)
        updateMediaSource()
    }

    private fun createMediaSource(uri: Uri): MediaSource? {
        return when (@ContentType Util.inferContentType(uri)) {
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(FileDataSource.Factory()).createMediaSource(uri)
            else -> {
                Timber.e("Unknown URI format, can't create MediaSource")
                null
            }
        }
    }

    private fun updateMediaSource(left: Float = 0.0f, right: Float = 1.0f) {
        lastStartUs = if (initialDuration == -1L) {
            0L
        } else {
            (initialDuration * left * 1000L).toLong()
        }
        lastEndUs = if (initialDuration == -1L) {
            C.TIME_END_OF_SOURCE
        } else {
            (initialDuration * right * 1000L).toLong()
        }

        val mediaSource = ClippingMediaSource(originalMediaSource, lastStartUs, lastEndUs)
        mediaSource.let {
            exoPlayer?.prepare(it)
            exoPlayer?.playWhenReady = true
        }
    }

    private fun selectFileToSave() {
        saveOnDestroy = true
        playOnAppResume = false
        val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        exportIntent.addCategory(Intent.CATEGORY_OPENABLE)
        exportIntent.type = "video/mp4"
        exportIntent.putExtra(Intent.EXTRA_TITLE, VIDEO_FILE_NAME_TO_SAVE)
        startActivityForResult(exportIntent, FILE_EXPORT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_EXPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.let {
                val uri = data.data
                uri?.let {
                    requireContext().contentResolver
                        .openFileDescriptor(uri, "rwt")?.fileDescriptor?.let {
                        compositeDisposable += videoEditor.saveCuttedFile(it)
                            .doOnEvent {
                                playOnAppResume = true
                            }
                            .subscribe({
                                parentFragmentManager.popBackStack()
                                Toast.makeText(requireContext(), R.string.file_saved, Toast.LENGTH_SHORT).show()
                            }, {
                                exoPlayer?.playWhenReady = true
                                Toast.makeText(requireContext(), R.string.file_save_error, Toast.LENGTH_SHORT).show()
                            })
                    }
                }
            }
        }
    }
}