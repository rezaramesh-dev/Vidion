package app.onestack.vidion.View.Activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import app.onestack.vidion.Models.Video
import app.onestack.vidion.R
import app.onestack.vidion.Utils.savePositionListPref
import app.onestack.vidion.databinding.ActivityPlayerBinding
import app.onestack.vidion.databinding.MoreFeaturesBinding
import app.onestack.vidion.databinding.SpeedDialogBinding
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.lang.Math.abs
import java.text.DecimalFormat
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener,
    GestureDetector.OnGestureListener {

    lateinit var binding: ActivityPlayerBinding
    private var isSubtitle: Boolean = true
    private lateinit var playPauseBtn: ImageView
    private lateinit var fullScreenBtn: ImageView
    private lateinit var videoTitle: TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat

    companion object {
        private var audioManager: AudioManager? = null
        private var timer: Timer? = null
        private lateinit var player: ExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = 1
        private var repeat: Boolean = false
        private var isFullscreen: Boolean = false
        private var isLocked: Boolean = false
        lateinit var trackSelector: DefaultTrackSelector
        private var speed: Float = 1.0f
        var pipStatus: Int = 0
        lateinit var dialog: Dialog
        var nowPlayingId: String = ""
        private var brightness: Int = 0
        private var volume: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)

        videoTitle = findViewById(R.id.videoTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        fullScreenBtn = findViewById(R.id.fullScreenBtn)

        gestureDetectorCompat = GestureDetectorCompat(this, this)

        //for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            //controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        //for handling video file intent
        try {
            if (intent.data?.scheme.contentEquals("content")) {
                playerList = ArrayList()
                position = 0
                val cursor = contentResolver.query(
                    intent.data!!, arrayOf(MediaStore.Video.Media.DATA), null, null, null
                )
                cursor?.let {
                    it.moveToFirst()
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(path)
                    val video = Video(
                        id = "",
                        title = file.name,
                        duration = 0L,
                        artUri = Uri.fromFile(file),
                        path = path,
                        size = "",
                        folderName = "",
                        dateAdded = ""
                    )
                    playerList.add(video)
                    cursor.close()
                }
                createPlayer()
                initializeBinding()
            } else {
                initializeBinding()
                initializeLayout()
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeLayout() {

        when (intent.getStringExtra("class")) {
            "AllVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPlayer()
            }
            "FolderActivity" -> {
                playerList = ArrayList()
                playerList.addAll(FoldersActivity.currentFolderVideo)
                createPlayer()
            }
            "SearchedVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.searchList)
                createPlayer()
            }
            "SortVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.sortList)
                createPlayer()
            }
            "NowPlaying" -> {
                speed = 1.0f
                videoTitle.text = playerList[position].title
                doubleTapEnable()
                videoTitle.isSelected = true
                playVideo()
                playInFullScreen(enable = isFullscreen)
                seekBarFeature()
            }
        }

        if (repeat) findViewById<ImageView>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
        else findViewById<ImageView>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
    }

    @SuppressLint("PrivateResource", "SetTextI18n", "UseCompatLoadingForDrawables")
    private fun initializeBinding() {

        /*findViewById<FrameLayout>(R.id.forwardFL).setOnClickListener(DoubleClickListener(callback = object :
            DoubleClickListener.Callback {
            override fun doubleClicked() {
                binding.playerView.showController()
                findViewById<ImageButton>(R.id.forwardBtn).visibility = View.VISIBLE
                player.seekTo(player.currentPosition + 10000)
                moreTime = 0
            }
        }))

        findViewById<FrameLayout>(R.id.rewindFL).setOnClickListener(DoubleClickListener(callback = object :
            DoubleClickListener.Callback {
            override fun doubleClicked() {
                binding.playerView.showController()
                findViewById<ImageButton>(R.id.rewindBtn).visibility = View.VISIBLE
                player.seekTo(player.currentPosition - 10000)
                moreTime = 0
            }
        }))*/

        val customDialog =
            LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
        val bindingMF = MoreFeaturesBinding.bind(customDialog)

        dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
            .setOnCancelListener { playVideo() }
            .create()

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            pauseVideo()
            savePositionListPref(this, position = position)
            finish()
        }

        playPauseBtn.setOnClickListener {
            if (player.isPlaying) pauseVideo() else playVideo()
        }

        findViewById<ImageView>(R.id.nextBtn).setOnClickListener { nextPrevVideo() }
        findViewById<ImageView>(R.id.prevBtn).setOnClickListener { nextPrevVideo(isNext = false) }
        findViewById<ImageView>(R.id.repeatBtn).setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageView>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageView>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }

        fullScreenBtn.setOnClickListener {
            if (isFullscreen) {
                isFullscreen = false
                playInFullScreen(enable = false)
            } else {
                isFullscreen = true
                playInFullScreen(enable = true)
            }
        }

        findViewById<ImageView>(R.id.lockBtn).setOnClickListener {
            if (!isLocked) {
                //for hiding
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                findViewById<ImageView>(R.id.lockBtnOnLock).visibility = View.VISIBLE
            } else {
                //for showing
                isLocked = false
                binding.playerView.useController = true
                findViewById<ImageView>(R.id.lockBtn).setImageResource(R.drawable.ic_lock_open)
            }
        }


        findViewById<ImageView>(R.id.lockBtnOnLock).setOnClickListener {
            //for showing
            isLocked = false
            binding.playerView.useController = true
            binding.playerView.showController()
            findViewById<ImageView>(R.id.lockBtnOnLock).visibility = View.GONE
            findViewById<ImageView>(R.id.lockBtn).visibility = View.VISIBLE
        }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            pauseVideo()
            //playVideo()
            dialog.show()

            val audioTrack = ArrayList<String>()
            for (i in 0 until player.currentTrackGroups.length) {
                if (player.currentTrackGroups.get(i)
                        .getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT
                ) {
                    audioTrack.add(
                        Locale(
                            player.currentTrackGroups.get(i).getFormat(0).language.toString()
                        ).displayLanguage
                    )
                }
            }

            val tempTracks = audioTrack.toArray(arrayOfNulls<CharSequence>(audioTrack.size))

            bindingMF.audioTrack.setOnClickListener {
                dialog.dismiss()

                val audioTrack = ArrayList<String>()
                val audioList = ArrayList<String>()

                for (group in player.currentTracksInfo.trackGroupInfos) {
                    if (group.trackType == C.TRACK_TYPE_AUDIO) {
                        val groupInfo = group.trackGroup
                        for (i in 0 until groupInfo.length) {
                            audioTrack.add(groupInfo.getFormat(i).language.toString())
                            audioList.add(
                                "${audioList.size + 1}. " + Locale(groupInfo.getFormat(i).language.toString()).displayLanguage + " (${
                                    groupInfo.getFormat(
                                        i
                                    ).label
                                })"
                            )
                        }
                    }
                }
                if (audioList[0].contains("null")) audioList[0] = "1. Default Track"

                val tempTracks = audioList.toArray(arrayOfNulls<CharSequence>(audioList.size))

                val audioDialog = MaterialAlertDialogBuilder(
                    this,
                    R.style.alertDialog
                ).setTitle("Select Language").setBackground(getDrawable(R.drawable.back_white))
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("Off Audio") { self, _ ->

                        trackSelector.setParameters(
                            trackSelector.buildUponParameters().setRendererDisabled(
                                C.TRACK_TYPE_AUDIO, true
                            )
                        )
                        self.dismiss()
                    }.setItems(tempTracks) { _, position ->
                        Toast.makeText(this, audioList[position] + "Selected", Toast.LENGTH_SHORT)
                            .show()
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setRendererDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setPreferredAudioLanguage(audioTrack[position])
                        )
                        playVideo()
                    }.create()

                audioDialog.setOnCancelListener {
                    playVideo()
                }
                audioDialog.show()
                audioDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                // audioDialog.window?.setBackgroundDrawable(ColorDrawable(0xffffff))
            }

            bindingMF.subtitlesBtn.setOnClickListener {
                if (isSubtitle) {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, true
                        ).build()
                    Toast.makeText(this, "Subtitle Off", Toast.LENGTH_SHORT).show()
                    isSubtitle = false
                } else {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, false
                        ).build()
                    Toast.makeText(this, "Subtitle On", Toast.LENGTH_SHORT).show()
                    isSubtitle = true
                }
                dialog.dismiss()
                playVideo()
            }

            bindingMF.speedBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogS =
                    LayoutInflater.from(this).inflate(R.layout.speed_dialog, binding.root, false)
                val bindingS = SpeedDialogBinding.bind(customDialogS)
                val dialogS =
                    MaterialAlertDialogBuilder(this).setView(customDialogS).setCancelable(true)
                        .setPositiveButton("OK") { self, _ ->
                            playVideo()
                            self.dismiss()
                        }.setBackground(ColorDrawable(0xffffff)).create()
                dialogS.show()

                dialogS.setOnCancelListener {
                    playVideo()
                }

                bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                bindingS.minusBtn.setOnClickListener {
                    changeSpeed(isIncrement = false)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
                bindingS.plusBtn.setOnClickListener {
                    changeSpeed(isIncrement = true)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
            }

            bindingMF.sleepTimer.setOnClickListener {
                dialog.dismiss()
                if (timer != null) {
                    Toast.makeText(
                        this,
                        "Timer Already Running!!\n Close App to Rest Timer!!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    var sleepTime = 15
                    val customDialogS = LayoutInflater.from(this)
                        .inflate(R.layout.speed_dialog, binding.root, false)
                    val bindingS = SpeedDialogBinding.bind(customDialogS)
                    val dialogS =
                        MaterialAlertDialogBuilder(this).setView(customDialogS).setCancelable(true)
                            .setPositiveButton("OK") { self, _ ->
                                timer = Timer()
                                val task = object : TimerTask() {
                                    override fun run() {
                                        moveTaskToBack(true)
                                        exitProcess(1)
                                    }
                                }
                                timer!!.schedule(task, sleepTime * 60 * 1000.toLong())
                                self.dismiss()
                                playVideo()
                            }.setBackground(ColorDrawable(0xffffff)).create()
                    dialogS.show()

                    dialogS.setOnCancelListener {
                        playVideo()
                    }

                    bindingS.speedText.text = "$sleepTime Min"
                    bindingS.minusBtn.setOnClickListener {
                        if (sleepTime > 15) sleepTime -= 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                    bindingS.plusBtn.setOnClickListener {
                        if (sleepTime < 120) sleepTime += 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                }
            }

            bindingMF.pipModeBtn.setOnClickListener {
                pictureInPictureMode(dialog)
            }
        }
    }

    private fun pictureInPictureMode(dialog: Dialog) {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (status) {
                this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                dialog.dismiss()
                binding.playerView.hideController()
                playVideo()
                pipStatus = 0
            } else {
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:${packageName}")
                )
                startActivity(intent)
            }
        } else {
            Toast.makeText(
                this, "Feature Not Supported!!", Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
            playVideo()
        }
    }

    private fun createPlayer() {
        try {
            player.release()
        } catch (e: Exception) {
            Log.i("Error", e.message.toString())
        }
        speed = 1.0f
        trackSelector = DefaultTrackSelector(this)
        videoTitle.text = playerList[position].title
        videoTitle.isSelected = true
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        doubleTapEnable()
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    pauseVideo()
                    savePositionListPref(this@PlayerActivity, position = position)
                    finish()
                    //nextPrevVideo()
                }
            }
        })
        playInFullScreen(enable = isFullscreen)
        //setVisibility()
        nowPlayingId = playerList[position].id

        seekBarFeature()
    }

    private fun playVideo() {
        playPauseBtn.setImageResource(R.drawable.ic_pause)
        player.play()
    }

    private fun pauseVideo() {
        playPauseBtn.setImageResource(R.drawable.ic_play)
        player.pause()
    }

    private fun nextPrevVideo(isNext: Boolean = true) {
        if (isNext) setPosition() else setPosition(isIncrement = false)
        createPlayer()
    }

    private fun setPosition(isIncrement: Boolean = true) {
        if (!repeat) {
            if (isIncrement) if (playerList.size - 1 == position) position = 0 else ++position
            else if (position == 0) position = playerList.size - 1 else --position
        }
    }

    private fun playInFullScreen(enable: Boolean) {
        if (enable) {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullScreenBtn.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenBtn.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    private fun changeSpeed(isIncrement: Boolean) {
        if (isIncrement) {
            if (speed < 2.9f) speed += 0.10f
        } else if (speed > 0.20f) speed -= 0.10f

        player.setPlaybackSpeed(speed)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("class", "FolderActivity")
                2 -> intent.putExtra("class", "SearchedVideos")
                3 -> intent.putExtra("class", "SortVideos")
                4 -> intent.putExtra("class", "AllVideos")
            }
            startActivity(intent)
        }
        if (!isInPictureInPictureMode) pauseVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        audioManager?.abandonAudioFocus { this }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        //if (focusChange <= 0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        if (audioManager == null) audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(
            this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )

        if (brightness != 0) setScreenBrightness(brightness)

        playVideo()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()
        try {
            if (!isInPictureInPictureMode) pauseVideo()
            else playVideo()
        } catch (_: Exception) {
        }
    }

    private fun screenOrientation() {
        requestedOrientation =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doubleTapEnable() {
        binding.playerView.player = player
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }
        })
        binding.ytOverlay.player(player)
        binding.playerView.setOnTouchListener { _, motionEvent ->

            binding.playerView.isDoubleTapEnabled = false
            if (!isLocked) {
                binding.playerView.isDoubleTapEnabled = true
                gestureDetectorCompat.onTouchEvent(motionEvent)
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    binding.icBrightness.visibility = View.GONE
                    binding.icVolume.visibility = View.GONE

                    //for immersive mode
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun seekBarFeature() {
        findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object :
            TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                pauseVideo()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playVideo()
            }
        })
    }

    override fun onDown(p0: MotionEvent): Boolean = false

    override fun onShowPress(p0: MotionEvent) = Unit

    override fun onSingleTapUp(p0: MotionEvent): Boolean = false

    override fun onLongPress(p0: MotionEvent) = Unit
    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean= false

    override fun onScroll(
        event: MotionEvent?, event1: MotionEvent, distanceX: Float, distanceY: Float
    ): Boolean {
        if (event != null){
            val sWidth = Resources.getSystem().displayMetrics.widthPixels
            val sHeight = Resources.getSystem().displayMetrics.heightPixels

            val border = 100 * Resources.getSystem().displayMetrics.density.toInt()
            if (event.x < border || event.y < border || event.x > sWidth - border || event.y > sHeight - border) return false

            if (abs(distanceX) < abs(distanceY)) {
                if (event.x < sWidth / 2) {
                    binding.icBrightness.visibility = View.VISIBLE
                    binding.icVolume.visibility = View.GONE
                    val increase = distanceY > 0
                    val newValue = if (increase) brightness + 1 else brightness - 1
                    if (newValue in 0..30) brightness = newValue
                    binding.icBrightness.text = brightness.toString()
                    setScreenBrightness(brightness)
                } else {
                    binding.icBrightness.visibility = View.GONE
                    binding.icVolume.visibility = View.VISIBLE

                    val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val increase = distanceY > 0
                    val newValue = if (increase) volume + 1 else volume - 1
                    if (newValue in 0..30) volume = newValue
                    binding.icBrightness.text = volume.toString()
                    audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                }
            }
        }
        return true
    }

    private fun setScreenBrightness(value: Int) {
        val d = 1.0f / 30
        val lp = this.window.attributes
        lp.screenBrightness = d * value
        this.window.attributes = lp
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!isLocked) super.onBackPressed()
        savePositionListPref(this, position)
    }
}