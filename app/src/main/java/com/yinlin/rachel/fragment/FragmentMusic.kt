package com.yinlin.rachel.fragment

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.yinlin.rachel.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.activity.VideoActivity
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.clearAddAll
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LoadMusicPreview
import com.yinlin.rachel.data.music.LoadMusicPreviewList
import com.yinlin.rachel.data.music.LyricsInfo
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.MusicInfoPreview
import com.yinlin.rachel.data.music.MusicInfoPreviewList
import com.yinlin.rachel.data.music.MusicPlayMode
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.databinding.FragmentMusicBinding
import com.yinlin.rachel.deleteFilter
import com.yinlin.rachel.dialog.BottomDialogCurrentPlaylist
import com.yinlin.rachel.dialog.BottomDialogLyricsEngine
import com.yinlin.rachel.dialog.BottomDialogLyricsInfo
import com.yinlin.rachel.dialog.BottomDialogMusicInfo
import com.yinlin.rachel.dialog.BottomDialogSleepMode
import com.yinlin.rachel.div
import com.yinlin.rachel.model.RachelAppIntent
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelMod
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.model.RachelTimer
import com.yinlin.rachel.model.engine.LyricsEngineFactory
import com.yinlin.rachel.pathMusic
import com.yinlin.rachel.pureColor
import com.yinlin.rachel.readJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentMusic(main: MainActivity) : RachelFragment<FragmentMusicBinding>(main), Player.Listener {
    companion object {
        const val UPDATE_FREQUENCY: Long = 100L // 更新频率

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
        const val GROUP_TOOL_SHARE = 4
        const val GROUP_TOOL_INFO = 5
    }

    private val musicInfos = HashMap<String, MusicInfo>() // 曲库集
    private val playlists = Config.playlist // 歌单集
    private val loadMusics = mutableListOf<String>() // 加载媒体集
    private var currentPlaylist: Playlist? = null // 当前播放列表
    private var savedMusic: MusicInfo? = null // 当前记录音乐

    private lateinit var onTimeUpdate: Runnable
    private lateinit var player: MediaController
    private var isPlayerInit: Boolean = false

    val sleepModeTimer = RachelTimer()

    private val bottomDialogLyricsEngine = BottomDialogLyricsEngine(this)
    private val bottomDialogCurrentPlaylist = BottomDialogCurrentPlaylist(this)
    private val bottomDialogLyricsInfo = BottomDialogLyricsInfo(this)
    private val bottomDialogMusicInfo = BottomDialogMusicInfo(this)
    private val bottomDialogSleepMode = BottomDialogSleepMode(this)

    override fun bindingClass() = FragmentMusicBinding::class.java

    override fun init() {
        v.headerContainer.listener = { pos -> when (pos) {
            GROUP_HEADER_LIBRARY -> main.navigate(FragmentLibrary(main, musicInfos.map {
                val value = it.value
                MusicInfoPreview(value.version, value.id, value.name, value.singer)
            }))
            GROUP_HEADER_PLAYLIST -> main.navigate(FragmentPlaylist(main, playlists.map { it.key }))
            GROUP_HEADER_LYRICS -> bottomDialogLyricsEngine.update().show()
            GROUP_HEADER_MOD -> RachelDialog.choice(main, "跳转工坊资源QQ群", listOf("专辑EP合集", "专辑EP", "单曲集")) {
                RachelAppIntent.QQGroup(main.rs(when (it) {
                    0 -> R.string.qqgroup_mod0
                    1 -> R.string.qqgroup_mod1
                    2 -> R.string.qqgroup_mod2
                    else -> R.string.qqgroup_main
                })).start(main)
            }
            GROUP_HEADER_SLEEP_MODE -> bottomDialogSleepMode.update().show()
        } }
    }

    override fun quit() {
        bottomDialogLyricsEngine.release()
        bottomDialogCurrentPlaylist.release()
        bottomDialogLyricsInfo.release()
        bottomDialogMusicInfo.release()
        bottomDialogSleepMode.release()

        if (isPlayerInit) {
            endTimeUpdate()
            player.removeListener(this)
        }
        isPlayerInit = false
    }

    override fun start() {
        // 加载曲库
        lifecycleScope.launch {
            val loading = main.loading
            withContext(Dispatchers.IO) {
                pathMusic.listFiles { file ->
                    file.isFile() && file.getName().lowercase().endsWith(RachelMod.RES_INFO)
                }?.let {
                    for (f in it) {
                        try {
                            val info: MusicInfo = f.readJson()
                            if (info.isCorrect) {
                                info.parseLyricsText()
                                musicInfos[info.id] = info
                            }
                        }
                        catch (_: Exception) { }
                    }
                }
            }
            loading.dismiss()
        }
    }

    // 设置播放模式
    private fun updatePlayMode(mode: MusicPlayMode) {
        when (mode) {
            MusicPlayMode.ORDER -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
                v.controlContainer.setItemImage(GROUP_CONTROL_MODE, R.drawable.icon_play_mode_order)
            }
            MusicPlayMode.LOOP -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
                v.controlContainer.setItemImage(GROUP_CONTROL_MODE, R.drawable.icon_play_mode_loop)
            }
            MusicPlayMode.RANDOM -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
                v.controlContainer.setItemImage(GROUP_CONTROL_MODE, R.drawable.icon_player_mode_random)
            }
        }
    }

    // 获得播放器服务
    private fun preparePlayer(controller: MediaController) {
        player = controller
        isPlayerInit = true
        player.addListener(this)
        updatePlayMode(Config.music_play_mode)

        // 更新播放进度回调
        onTimeUpdate = object : Runnable {
            override fun run() {
                val position = player.currentPosition
                v.progress.updateProgress(position, false) // 更新进度条
                v.lyrics.update(position) // 更新歌词
                postDelay(UPDATE_FREQUENCY, this) // 更新消息
            }
        }
        // 进度条
        v.progress.setOnProgressChangedListener {
            // 计算按下位置占总进度条的百分比, 来同比例到时长上
            player.seekTo((player.duration * it).toLong())
            if (!player.isPlaying) player.play()
        }

        v.controlContainer.listener = { pos -> when (pos) {
            GROUP_CONTROL_MODE -> when (Config.music_play_mode) {
                MusicPlayMode.ORDER -> { // 顺序播放
                    Config.music_play_mode = MusicPlayMode.LOOP
                    updatePlayMode(MusicPlayMode.LOOP)
                }
                MusicPlayMode.LOOP -> { // 单曲循环
                    Config.music_play_mode = MusicPlayMode.RANDOM
                    updatePlayMode(MusicPlayMode.RANDOM)
                }
                MusicPlayMode.RANDOM -> { // 随机播放
                    Config.music_play_mode = MusicPlayMode.ORDER
                    updatePlayMode(MusicPlayMode.ORDER)
                }
            }
            GROUP_CONTROL_PREVIOUS -> if (isLoadMusic) player.seekToPreviousMediaItem()
            GROUP_CONTROL_PLAY -> if (isLoadMusic) { if (player.isPlaying) player.pause() else player.play() }
            GROUP_CONTROL_NEXT -> if (isLoadMusic) player.seekToNextMediaItem()
            GROUP_CONTROL_PLAYLIST -> if (isLoadMusic) {
                val currentMusicInfo = currentMusic
                currentPlaylist?.let { playlist ->
                    bottomDialogCurrentPlaylist.update(playlist.name, playlist.items.map {
                        val musicInfo = musicInfos[it]
                        LoadMusicPreview(it, musicInfo?.name ?: it, musicInfo?.singer ?: "",
                            musicInfo == null, musicInfo == currentMusicInfo)
                    }).show()
                }
            }
        } }
        v.toolContainer.listener = { pos -> when (pos) {
            GROUP_TOOL_AN -> currentMusic?.let {
                if (it.bgd) {
                    val isBgd = v.bg.tag as Boolean
                    v.bg.load(if (isBgd) it.bgsPath else it.bgdPath)
                    v.bg.tag = !isBgd
                }
                else tip(Tip.WARNING, "此歌曲不支持壁纸动画")
            }
            GROUP_TOOL_MV -> currentMusic?.let {
                if (it.video) {
                    player.pause()
                    val intent = Intent(main, VideoActivity::class.java)
                    intent.putExtra("uri", it.videoPath.absolutePath)
                    startActivity(intent)
                }
                else tip(Tip.WARNING, "此歌曲不支持视频PV")
            }
            GROUP_TOOL_LYRICS -> currentMusic?.lyrics?.let {
                val arr = mutableListOf<LyricsInfo>()
                for ((engineName, nameList) in it) {
                    val available = LyricsEngineFactory.hasEngine(engineName)
                    for (name in nameList) arr += LyricsInfo(engineName, name, available)
                }
                bottomDialogLyricsInfo.update(arr).show()
            }
            GROUP_TOOL_COMMENT -> tip(Tip.INFO, "即将开放, 敬请期待新版本!")
            GROUP_TOOL_SHARE -> currentMusic?.let {
                RachelDialog.confirm(main, content="导出MOD\"${this.id}\"到文件分享?") {
                    shareMusic(it.id)
                }
            }
            GROUP_TOOL_INFO -> currentMusic?.let { bottomDialogMusicInfo.update(it).show() }
        } }
    }

    override fun update() {
        if (isPlayerInit) {
            // 进入前台时需要更新
            updateForeground()
            // 进入前台如果播放器是播放状态则启动更新回调
            if (player.isPlaying) startTimeUpdate()
        }
    }

    override fun hidden() {
        // 离开前台如果播放器是播放状态则停止更新回调
        if (isPlayerInit && player.isPlaying) endTimeUpdate()
    }

    @Suppress("UNCHECKED_CAST")
    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            RachelMessage.PREPARE_PLAYER -> preparePlayer(args[0] as MediaController)
            RachelMessage.MUSIC_START_PLAYER -> {
                val arg = args[0]
                if (arg is String) playlists[arg]?.let { startPlayer(it) }
                else if (arg is Playlist) startPlayer(arg)
            }
            RachelMessage.MUSIC_PAUSE_PLAYER -> if (isLoadMusic && player.isPlaying) player.pause()
            RachelMessage.MUSIC_STOP_PLAYER -> stopPlayer()
            RachelMessage.MUSIC_DELETE_PLAYLIST -> {
                val title = args[0] as String
                playlists[title]?.let {
                    // 检查歌单是否正在播放
                    if (isLoadPlaylist(it)) stopPlayer()
                    // UI更新
                    playlists.remove(it.name)
                    // 数据存储
                    Config.playlist = playlists
                }
            }
            RachelMessage.MUSIC_UPDATE_PLAYLIST -> {
                val title = args[0] as String
                val newItems = args[1] as LoadMusicPreviewList
                playlists[title]?.let {
                    it.items.clearAddAll(newItems.map { item -> item.id })
                    // 数据存储
                    Config.playlist = playlists
                }
            }
            RachelMessage.MUSIC_RELOAD_PLAYLIST -> playlists.clearAddAll(Config.playlist)
            RachelMessage.MUSIC_ADD_MUSIC_INTO_PLAYLIST -> {
                if (playlists.isEmpty()) {
                    tip(Tip.WARNING, "没有创建任何歌单")
                    return
                }
                // 获得所有歌单名供选择
                val playlistName = playlists.keys.toList()
                RachelDialog.choice(main, "添加到歌单", playlistName) { pos ->
                    val text = playlistName[pos]
                    val selectItems = args[0] as MusicInfoPreviewList
                    val playlist = playlists[text]!!
                    // 更新UI
                    val ids = playlist.items
                    var num = 0
                    val isPlaying = isLoadPlaylist(playlist)
                    for (selectItem in selectItems) {
                        val id = selectItem.id
                        if (!ids.contains(id)) { // 重复过滤
                            ++num
                            ids += id
                            // 检查当前是否在播放此列表
                            if (isPlaying) {
                                musicInfos[id]?.let {
                                    loadMusics += id
                                    player.addMediaItem(buildMusicItem(it))
                                }
                            }
                        }
                    }
                    // 数据存储
                    if (num > 0) Config.playlist = playlists
                    tip(Tip.SUCCESS, "已添加${num}首歌曲")
                }
            }
            RachelMessage.MUSIC_DELETE_MUSIC_FROM_PLAYLIST -> {
                val title = args[0] as String
                val id = args[1] as String
                playlists[title]?.let {
                    val position = it.items.indexOf(id)
                    if (position != -1) {
                        // 检查歌单是否正在播放
                        if (isLoadPlaylist(it)) {
                            val mediaIndex = loadMusics.indexOf(id)
                            // 如果是单曲循环, 则直接结束播放
                            if (player.repeatMode == Player.REPEAT_MODE_ONE) stopPlayer()
                            else {
                                loadMusics.removeAt(mediaIndex)
                                player.removeMediaItem(mediaIndex)
                            }
                        }
                        // UI更新
                        it.items.removeAt(position)
                        // 数据存储
                        Config.playlist = playlists
                    }
                }
            }
            RachelMessage.MUSIC_DELETE_MUSIC -> {
                val selectItems = args[0] as MusicInfoPreviewList
                currentPlaylist?.let {
                    val currentMusicId = currentMusic?.id
                    for (selectItem in selectItems) {
                        val id = selectItem.id
                        if (it.items.contains(id)) {
                            if (id == currentMusicId) {
                                // 删除歌曲正在播放，只能停止播放器
                                stopPlayer()
                                break
                            }
                            else {
                                val mediaIndex = loadMusics.indexOf(id)
                                // 如果是单曲循环, 则直接结束播放
                                if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                                    stopPlayer()
                                    break
                                }
                                else {
                                    loadMusics.removeAt(mediaIndex)
                                    player.removeMediaItem(mediaIndex)
                                }
                            }
                        }
                    }
                }
                for (selectItem in selectItems) {
                    val id = selectItem.id
                    // 更新UI
                    musicInfos.remove(id)
                    // 数据存储
                    pathMusic.deleteFilter(id)
                }
            }
            RachelMessage.MUSIC_GOTO_MUSIC -> {
                val index = loadMusics.indexOf(args[0] as String)
                if (player.currentMediaItemIndex != index) {
                    player.seekTo(index, 0)
                    if (!player.isPlaying) player.play()
                }
            }
            RachelMessage.MUSIC_NOTIFY_ADD_MUSIC -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        for (id in args[0] as List<*>) {
                            val info: MusicInfo = (pathMusic / (id as String + RachelMod.RES_INFO)).readJson()
                            if (info.isCorrect) {
                                info.parseLyricsText()
                                musicInfos[info.id] = info
                            }
                            else musicInfos.remove(id)
                        }
                    }
                }
            }
            RachelMessage.MUSIC_USE_LYRICS_ENGINE -> {
                val engineName = args[0] as String
                val name = args[1] as String
                currentMusic?.let {
                    if (!v.lyrics.switchEngine(it, engineName, name)) tip(Tip.ERROR, "加载歌词引擎失败")
                }
            }
            else -> {}
        }
    }

    override fun messageForResult(msg: RachelMessage, vararg args: Any?): Any? {
        when (msg) {
            RachelMessage.MUSIC_GET_PLAYLIST_INFO_PREVIEW -> {
                val title = args[0] as String
                return playlists[title]?.items?.map {
                    val musicInfo = musicInfos[it]
                    LoadMusicPreview(it, musicInfo?.name ?: it, musicInfo?.singer ?: "",
                        musicInfo == null, false)
                }
            }
            RachelMessage.MUSIC_CREATE_PLAYLIST -> {
                val newName = args[0] as String
                // 校验歌单
                if (newName.isEmpty() || playlists.containsKey(newName) ||
                    newName == main.rs(R.string.default_playlist_name)) return false
                // UI更新
                playlists[newName] = Playlist(newName, mutableListOf())
                // 数据存储
                Config.playlist = playlists
                return true
            }
            RachelMessage.MUSIC_RENAME_PLAYLIST -> {
                val title = args[0] as String
                val newName = args[1] as String
                val playlist = playlists[title] ?: return false
                // 校验歌单
                if (newName.isEmpty() || playlists.containsKey(newName) ||
                    newName == main.rs(R.string.default_playlist_name)) return false
                // UI更新
                playlists.remove(playlist.name)
                playlists[newName] = Playlist(newName, playlist.items)
                // 数据存储
                Config.playlist = playlists
                return true
            }
            RachelMessage.MUSIC_GET_MUSIC_INFO_PREVIEW -> {
                val name = args[0] as String?
                return (if (name != null) musicInfos.filter { it.key.contains(name,true) } else musicInfos).map {
                    val value = it.value
                    MusicInfoPreview(value.version, value.id, value.name, value.singer)
                }
            }
            else -> return null
        }
    }

    // 媒体切换
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        if (mediaItem != null) {
            // 加载歌词引擎
            musicInfos[mediaItem.mediaId]?.let {
                if(!v.lyrics.loadEngine(it)) tip(Tip.ERROR, "加载歌词引擎失败")
            }

            // 处于前台时更新前台信息
            if (isForeground) updateForeground()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        stopPlayer()
        RachelDialog.info(main,"播放器异常", error.toString())
    }

    // 媒体状态改变 (指播放器播放或停止的状态)
    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_READY) {
            if (isForeground) updateForeground() // 处于前台时更新前台信息
        } else if (playbackState == Player.STATE_ENDED) {
            if (isForeground) updateForeground() // 处于前台时更新前台信息
            // 置空当前播放歌单与歌曲
            currentPlaylist = null
            savedMusic = null
            // 停止歌词引擎
            v.lyrics.releaseEngine()
        }
    }

    // 播放状态改变 (指播放器进入播放或暂停的状态)
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        if (isPlaying) {
            v.controlContainer.setItemImage(GROUP_CONTROL_PLAY, R.drawable.icon_player_play)
            if (isForeground) startTimeUpdate()
        } else {
            v.controlContainer.setItemImage(GROUP_CONTROL_PLAY, R.drawable.icon_player_pause)
            if (isForeground) endTimeUpdate()
        }
    }

    // 是否在前台
    private val isForeground get() = main.isForeground(RachelTab.music)
    // 取当前播放音乐
    private val currentMusic: MusicInfo? get() = player.currentMediaItem?.let { musicInfos[it.mediaId] }
    // 是否加载音乐 (即player加载了music的状态, music处于播放或暂停)
    private val isLoadMusic get() = player.playbackState == Player.STATE_READY && currentMusic != null
    // 是否在播放某个歌单
    private fun isLoadPlaylist(playlist: Playlist) = isLoadMusic && currentPlaylist == playlist

    // 开始前台更新 (播放器启动时间刻更新)
    private fun startTimeUpdate() {
        post(onTimeUpdate)
        v.record.startCD()
    }

    // 停止前台更新 (播放器停止时间刻更新)
    private fun endTimeUpdate() {
        removePost(onTimeUpdate)
        v.record.pauseCD()
    }

    private fun buildMusicItem(musicInfo: MusicInfo): MediaItem {
        return MediaItem.Builder()
            .setMediaId(musicInfo.id)
            .setUri(Uri.fromFile(musicInfo.audioPath))
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(musicInfo.name)
                .setArtist(musicInfo.singer)
                .setAlbumTitle(musicInfo.album)
                .setAlbumArtist(musicInfo.singer)
                .setComposer(musicInfo.composer)
                .setWriter(musicInfo.lyricist)
                .setArtworkUri(Uri.fromFile(musicInfo.recordPath))
                .build())
            .build()
    }

    // 播放歌单
    private fun startPlayer(playlist: Playlist) {
        loadMusics.clear() // 清空已装载音乐信息
        val mediaItems = mutableListOf<MediaItem>()
        for (id in playlist.items) {
            musicInfos[id]?.let {
                loadMusics += id
                mediaItems += buildMusicItem(it)
            }
        }
        currentPlaylist = playlist // 设置当前播放歌单

        // 启动 Player
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
    }

    // 停止播放
    private fun stopPlayer() {
        if (isLoadMusic) {
            // 清理已装载音乐信息
            player.clearMediaItems()
            loadMusics.clear()
        }
    }

    // 更新前台
    private fun updateForeground() {
        // 更新背景
        val info = currentMusic
        v.progress.setInfo(info?.chorus ?: emptyList(), if (player.duration == C.TIME_UNSET) 0 else player.duration)
        if (info == null) { // 停止播放状态, 更新
            // 更新歌曲信息
            v.title.text = main.rs(R.string.no_audio_source)
            v.singer.text = ""
            v.record.clearCD()
            v.bg.tag = false
            v.bg.pureColor = main.rc(R.color.black)
            v.toolContainer.setItemImage(GROUP_TOOL_AN, R.drawable.icon_an_off)
            v.toolContainer.setItemImage(GROUP_TOOL_MV, R.drawable.icon_mv_off)
            // 更新已播放进度与进度条
            v.progress.updateProgress(0L, true)
        }
        else {
            v.progress.updateProgress(player.currentPosition, true)
            if (info != savedMusic) { // 只有与之前音乐不同才更新
                savedMusic = info
                // 更新歌曲信息
                v.title.text = info.name
                v.singer.text = info.singer
                v.record.loadCD(info.recordPath)
                v.bg.tag = info.bgd
                v.bg.load(if (info.bgd) info.bgdPath else info.bgsPath)
                v.toolContainer.setItemImage(GROUP_TOOL_AN, if (info.bgd) R.drawable.icon_an_on else R.drawable.icon_an_off)
                v.toolContainer.setItemImage(GROUP_TOOL_MV, if (info.video) R.drawable.icon_mv_on else R.drawable.icon_mv_off)
                // 已播放进度和进度条由onTimeUpdate更新, 不用在此更新
            }
        }
    }

    // 分享歌曲
    @NewThread
    private fun shareMusic(id: String) {
        lifecycleScope.launch {
            val loading = main.loading
            withContext(Dispatchers.IO) {
                try {
                    val merger = RachelMod.Merger(pathMusic)
                    val metadata = merger.getMetadata(listOf(id), emptyList(), null)
                    val resolver: ContentResolver = main.contentResolver
                    val values = ContentValues()
                    values.put(MediaStore.Downloads.DISPLAY_NAME, "${id}.rachel")
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                    if (resolver.openOutputStream(uri).use { merger.run(it!!, metadata, null) }) {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "application/*"
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        startActivity(Intent.createChooser(intent, "分享歌曲MOD"))
                        return@withContext
                    }
                }
                catch (_: Exception) { }
                tip(Tip.ERROR, "导出MOD失败")
            }
            loading.dismiss()
        }
    }
}