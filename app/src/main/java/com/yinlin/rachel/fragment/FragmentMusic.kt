package com.yinlin.rachel.fragment


import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.activity.VideoActivity
import com.yinlin.rachel.common.MusicCenter
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.Command
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.data.music.LyricsInfo
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.MusicInfoPreviewList
import com.yinlin.rachel.data.music.MusicPlayMode
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.data.music.PlaylistMap
import com.yinlin.rachel.data.music.PlaylistPreview
import com.yinlin.rachel.databinding.FragmentMusicBinding
import com.yinlin.rachel.model.RachelAppIntent
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelTimer
import com.yinlin.rachel.model.engine.LyricsEngineFactory
import com.yinlin.rachel.sheet.SheetCurrentPlaylist
import com.yinlin.rachel.sheet.SheetLyricsEngine
import com.yinlin.rachel.sheet.SheetLyricsInfo
import com.yinlin.rachel.sheet.SheetSleepMode
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.tool.pureColor
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.readText
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.view.FloatingLyricsView
import kotlinx.coroutines.launch

class FragmentMusic(main: MainActivity) : RachelFragment<FragmentMusicBinding>(main), MusicCenter.UIListener {
    companion object {
        const val GROUP_HEADER_LIBRARY = 0
        const val GROUP_HEADER_PLAYLIST = 1
        const val GROUP_HEADER_LYRICS = 2
        const val GROUP_HEADER_MOD = 3
        const val GROUP_HEADER_SLEEP_MODE = 4

        const val GROUP_CONTROL_MODE = 0
        const val GROUP_CONTROL_PREVIOUS = 1
        const val GROUP_CONTROL_PLAY = 2
        const val GROUP_CONTROL_NEXT = 3
        const val GROUP_CONTROL_PLAYLIST = 4

        const val GROUP_TOOL_AN = 0
        const val GROUP_TOOL_MV = 1
        const val GROUP_TOOL_LYRICS = 2
        const val GROUP_TOOL_COMMENT = 3
    }

    lateinit var musicCenter: MusicCenter
    private val floatingLyrics = FloatingLyricsView(main)
    private var isForeground: Boolean = false
    private val sleepModeTimer = RachelTimer()

    override fun bindingClass() = FragmentMusicBinding::class.java

    override fun quit() {
        sleepModeTimer.cancel()
        musicCenter.release()
    }

    override fun start() {
        v.headerContainer.listener = { pos -> when (pos) {
            GROUP_HEADER_LIBRARY -> main.navigate(FragmentLibrary(main, musicCenter.previewLibrary))
            GROUP_HEADER_PLAYLIST -> main.navigate(FragmentPlaylist(main, musicCenter.playlistNames))
            GROUP_HEADER_LYRICS -> SheetLyricsEngine(this).show()
            GROUP_HEADER_MOD -> RachelDialog.choice(main, "跳转工坊资源QQ群", listOf("专辑EP合集", "专辑EP", "单曲集")) {
                RachelAppIntent.QQGroup(main.rs(when (it) {
                    0 -> R.string.qqgroup_mod0
                    1 -> R.string.qqgroup_mod1
                    2 -> R.string.qqgroup_mod2
                    else -> R.string.qqgroup_main
                })).start(main)
            }
            GROUP_HEADER_SLEEP_MODE -> prepareSleepMode()
        } }

        v.controlContainer.listener = { pos -> when (pos) {
            GROUP_CONTROL_MODE -> musicCenter.send(Command.CommandNextMode)
            GROUP_CONTROL_PREVIOUS -> musicCenter.gotoPrevious()
            GROUP_CONTROL_PLAY -> musicCenter.send(Command.CommandPlayOrPause)
            GROUP_CONTROL_NEXT -> musicCenter.gotoNext()
            GROUP_CONTROL_PLAYLIST -> musicCenter.withPlaylist {
                val data = musicCenter.previewCurrentPlaylist
                if (data.isNotEmpty()) SheetCurrentPlaylist(this, it.name, data).show()
            }
        } }

        v.progress.setOnProgressChangedListener {
            musicCenter.send(Command.CommandSetProgressPercent, Bundle().apply { putFloat(Command.ARG_PROGRESS_PERCENT, it) })
        }

        v.record.rachelClick {
            musicCenter.withMusic { main.navigate(FragmentMusicInfo(main, it)) }
        }

        v.toolContainer.listener = { pos -> when (pos) {
            GROUP_TOOL_AN -> musicCenter.withMusic {
                if (it.bgd) {
                    val isBgd = v.bg.tag as Boolean
                    v.bg.load(if (isBgd) it.bgsPath else it.bgdPath)
                    v.bg.tag = !isBgd
                }
                else tip(Tip.WARNING, "此歌曲不支持壁纸动画")
            }
            GROUP_TOOL_MV -> musicCenter.withMusic {
                if (it.video) {
                    musicCenter.send(Command.CommandPause)
                    val intent = Intent(main, VideoActivity::class.java)
                    intent.putExtra("uri", it.videoPath.absolutePath)
                    startActivity(intent)
                }
                else tip(Tip.WARNING, "此歌曲不支持视频PV")
            }
            GROUP_TOOL_LYRICS -> musicCenter.withMusic {
                val arr = mutableListOf<LyricsInfo>()
                for ((engineName, nameList) in it.lyrics) {
                    val available = LyricsEngineFactory.hasEngine(engineName)
                    for (name in nameList) arr += LyricsInfo(engineName, name, available)
                }
                SheetLyricsInfo(this, arr).show()
            }
            GROUP_TOOL_COMMENT -> tip(Tip.INFO, "即将开放, 敬请期待新版本!")
        } }

        // 更新播放模式
        onMusicModeChanged(musicCenter.playMode)
        // 更新歌曲信息
        isForeground = true
        updateMusicInfo(musicCenter.currentMusicInfo)
    }

    override fun update() {
        isForeground = true
        updateMusicInfo(musicCenter.currentMusicInfo)
    }

    override fun hidden() {
        isForeground = false
    }

    override fun back() = BackState.HOME

    @Suppress("UNCHECKED_CAST")
    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            RachelMessage.MUSIC_START_PLAYER -> musicCenter.start(args[0] as Playlist, if (args.size == 2) args[1] as String else null)
            RachelMessage.MUSIC_STOP_PLAYER -> musicCenter.send(Command.CommandStop)
            RachelMessage.MUSIC_DELETE_PLAYLIST -> {
                val playlist = musicCenter.findPlaylist(args[0] as String)
                playlist?.let { musicCenter.deletePlaylist(it) }
            }
            RachelMessage.MUSIC_MOVE_MUSIC_IN_PLAYLIST -> {
                val playlist = musicCenter.findPlaylist(args[0] as String)
                playlist?.let { musicCenter.moveMusicInPlaylist(it, args[1] as Int, args[2] as Int) }
            }
            RachelMessage.MUSIC_RELOAD_PLAYLIST -> musicCenter.reloadPlaylist(args[0] as PlaylistMap)
            RachelMessage.MUSIC_DELETE_MUSIC_FROM_PLAYLIST -> {
                val playlist = musicCenter.findPlaylist(args[0] as String)
                playlist?.let { musicCenter.deleteMusicFromPlaylist(it, args[1] as String) }
            }
            RachelMessage.MUSIC_NOTIFY_ADD_MUSIC -> lifecycleScope.launch { musicCenter.notifyAddMusic(args[0] as List<String>) }
            RachelMessage.MUSIC_DELETE_MUSIC -> lifecycleScope.launch { musicCenter.deleteMusic(args[0] as MusicInfoPreviewList) }
            RachelMessage.MUSIC_GOTO_MUSIC -> {
                val index = musicCenter.indexOfMusic(args[0] as String)
                musicCenter.send(Command.CommandGotoIndex, Bundle().apply { putInt(Command.ARG_INDEX, index) })
            }
            RachelMessage.MUSIC_UPDATE_MUSIC_INFO -> updateMusicInfo(args[0] as MusicInfo)
            RachelMessage.MUSIC_USE_LYRICS_ENGINE -> {
                val engineName = args[0] as String
                val name = args[1] as String
                musicCenter.withMusic {
                    if (!v.lyrics.switchEngine(it, engineName, name)) tip(Tip.ERROR, "加载歌词引擎失败")
                }
            }
            RachelMessage.MUSIC_PREPARE_FLOATING_LYRICS -> prepareFlowLyrics(musicCenter.currentMusicInfo)
            RachelMessage.MUSIC_UPDATE_LYRICS_SETTINGS -> floatingLyrics.updateSettings(Config.music_lyrics_settings)
            else -> {}
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun messageForResult(msg: RachelMessage, vararg args: Any?): Any? {
        when (msg) {
            RachelMessage.MUSIC_GET_PLAYLIST_NAMES -> return musicCenter.playlistNames
            RachelMessage.MUSIC_GET_PLAYLIST_PREVIEW -> {
                val name = args[0] as String
                val playlist = musicCenter.findPlaylist(name)
                return playlist?.let { musicCenter.previewPlaylist(it) } ?: PlaylistPreview(name, emptyList())
            }
            RachelMessage.MUSIC_FIND_PLAYLIST -> return musicCenter.findPlaylist(args[0] as String)
            RachelMessage.MUSIC_CREATE_PLAYLIST -> return musicCenter.createPlaylist(args[0] as String)
            RachelMessage.MUSIC_RENAME_PLAYLIST -> {
                val playlist = musicCenter.findPlaylist(args[0] as String)
                return playlist?.let { musicCenter.renamePlaylist(it, args[1] as String) } ?: false
            }
            RachelMessage.MUSIC_GET_CURRENT_PLAYLIST_PREVIEW -> return musicCenter.previewCurrentPlaylist
            RachelMessage.MUSIC_ADD_MUSIC_INTO_PLAYLIST -> {
                val playlist = musicCenter.findPlaylist(args[0] as String)
                val selectItems = args[1] as MusicInfoPreviewList
                return playlist?.let { musicCenter.addMusicIntoPlaylist(playlist, selectItems) } ?: 0
            }
            RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO -> return musicCenter.currentMusicInfo
            RachelMessage.MUSIC_GET_MUSIC_INFO -> return musicCenter.findMusic(args[0] as String)
            RachelMessage.MUSIC_SEARCH_MUSIC_INFO_PREVIEW -> return musicCenter.searchMusicPreview(args[0] as String?)
            else -> return null
        }
    }
    
    override fun onMusicModeChanged(mode: MusicPlayMode) {
        v.controlContainer.setItemImage(GROUP_CONTROL_MODE, when (mode) {
            MusicPlayMode.LOOP -> R.drawable.icon_play_mode_loop
            MusicPlayMode.RANDOM -> R.drawable.icon_player_mode_random
            else -> R.drawable.icon_play_mode_order
        })
    }

    override fun onMusicUpdate(position: Long) {
        // 位于前台时更新进度条与歌词
        if (isForeground) updateProgress(position, false)
        // 更新状态栏歌词
        if (floatingLyrics.needUpdate(position)) floatingLyrics.update(position)
    }

    override fun onMusicReady(musicInfo: MusicInfo, current: Long, duration: Long) {
        updateProgress(current, true)
        v.progress.setInfo(musicInfo.chorus, duration)
    }

    override fun onMusicChanged(musicInfo: MusicInfo?) {
        // 更新歌曲信息
        updateMusicInfo(musicInfo)

        // 加载歌词引擎
        musicInfo?.let {
            // 加载歌词
            if (it.lrcData == null) it.lrcData = LrcData.parseLrcData(it.defaultLrcPath.readText())
            // 加载歌词引擎
            if (!v.lyrics.loadEngine(it)) tip(Tip.ERROR, "加载歌词引擎失败")
            // 处理悬浮歌词
            prepareFlowLyrics(it)
        }
    }

    override fun onMusicPlaying(isPlaying: Boolean) {
        if (isPlaying) {
            v.record.startCD()
            v.controlContainer.setItemImage(GROUP_CONTROL_PLAY, R.drawable.icon_player_play)
        }
        else {
            v.record.pauseCD()
            v.controlContainer.setItemImage(GROUP_CONTROL_PLAY, R.drawable.icon_player_pause)
        }
    }

    override fun onMusicStop() {
        // 停止歌词引擎
        v.progress.setInfo(emptyList(), 0L)
        v.lyrics.releaseEngine()
        floatingLyrics.clear()
    }

    override fun onMusicError(error: PlaybackException) {
        RachelDialog.info(main,"播放器异常", error.toString())
    }

    private fun updateProgress(position: Long, immediately: Boolean = false) {
        v.progress.updateProgress(position, immediately) // 更新进度条
        v.lyrics.update(position) // 更新歌词
    }

    private fun updateMusicInfo(musicInfo: MusicInfo?) {
        if (musicInfo == null) { // 停止播放状态, 更新
            // 更新歌曲信息
            v.title.text = main.rs(R.string.no_audio_source)
            v.singer.text = ""
            v.record.loadCD(null)
            v.bg.tag = false
            v.bg.pureColor = main.rc(R.color.black)
            v.toolContainer.setItemImageTint(GROUP_TOOL_AN, main.rc(R.color.white))
            v.toolContainer.setItemImageTint(GROUP_TOOL_MV, main.rc(R.color.white))
        }
        else {
            v.title.text = musicInfo.name
            v.singer.text = musicInfo.singer
            v.record.loadCD(musicInfo.recordPath)
            v.bg.tag = musicInfo.bgd
            v.bg.load(if (musicInfo.bgd) musicInfo.bgdPath else musicInfo.bgsPath)
            v.toolContainer.setItemImageTint(GROUP_TOOL_AN, main.rc(if (musicInfo.bgd) R.color.steel_blue else R.color.white))
            v.toolContainer.setItemImageTint(GROUP_TOOL_MV, main.rc(if (musicInfo.video) R.color.steel_blue else R.color.white))
        }
    }

    // 睡眠模式
    private fun prepareSleepMode() {
        if (sleepModeTimer.isStart) SheetSleepMode(this, sleepModeTimer).show()
        else {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(0).setMinute(0)
                .setTitleText("睡眠定时")
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build()
            picker.addOnPositiveButtonClickListener {
                val minutes = picker.hour * 60 + picker.minute
                if (minutes > 0L) {
                    sleepModeTimer.start(minutes * 60 * 1000L, 1000L) { musicCenter.send(Command.CommandStop) }
                    tip(Tip.SUCCESS, "睡眠模式已开启")
                }
            }
            picker.show(main.supportFragmentManager, SheetSleepMode::class.java.name)
        }
    }

    // 启动悬浮窗
    private fun prepareFlowLyrics(musicInfo: MusicInfo?) {
        if (floatingLyrics.canShow) {
            if (!floatingLyrics.isAttached) {
                val manager = main.getSystemService(Context.WINDOW_SERVICE) as? WindowManager?
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT)
                params.gravity = Gravity.TOP
                floatingLyrics.updateSettings(Config.music_lyrics_settings)
                manager?.addView(floatingLyrics, params)
            }
            musicInfo?.lrcData?.let { floatingLyrics.load(it) }
            floatingLyrics.showState = true
        }
        else {
            if (floatingLyrics.isAttached) {
                val manager = main.getSystemService(Context.WINDOW_SERVICE) as? WindowManager?
                manager?.removeView(floatingLyrics)
            }
            floatingLyrics.showState = false
        }
    }
}