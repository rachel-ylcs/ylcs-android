package com.yinlin.rachel.common


import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.MusicInfoPreviewList
import com.yinlin.rachel.data.music.MusicMap
import com.yinlin.rachel.data.music.MusicPlayMode
import com.yinlin.rachel.data.music.MusicRes
import com.yinlin.rachel.data.music.PlayingMusicPreview
import com.yinlin.rachel.data.music.PlayingMusicPreviewList
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.data.music.PlaylistMap
import com.yinlin.rachel.data.music.PlaylistPreview
import com.yinlin.rachel.service.MusicService
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.tool.deleteFilter
import com.yinlin.rachel.tool.moveItem
import com.yinlin.rachel.tool.pathMusic
import com.yinlin.rachel.tool.readJson
import com.yinlin.rachel.tool.rs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


typealias RachelPlayer = MediaController

class MusicCenter(private val context: Context, private val handler: Handler, private val uiListener: UIListener) : Player.Listener {
    companion object {
        const val UPDATE_FREQUENCY: Long = 100L // 更新频率
    }

    interface UIListener {
        fun onMusicModeChanged(mode: MusicPlayMode)
        fun onMusicChanged(musicInfo: MusicInfo?)
        fun onMusicReady(musicInfo: MusicInfo, current: Long, duration: Long)
        fun onMusicPlaying(isPlaying: Boolean)
        fun onMusicUpdate(position: Long)
        fun onMusicStop()
        fun onMusicError(error: PlaybackException)
    }

    private lateinit var player: RachelPlayer // 播放器

    // 曲库集
    private val musicInfos: MusicMap = mutableMapOf()
    // 查找歌曲
    fun findMusic(id: String): MusicInfo? = musicInfos[id]
    // 模糊搜索歌曲
    fun searchMusicPreview(key: String?): MusicInfoPreviewList = (if (key == null) musicInfos else
        musicInfos.filter { it.value.name.contains(key, true) }).map { it.value.preview }
    // 曲库所有歌曲预览
    val previewLibrary get(): MusicInfoPreviewList = musicInfos.map { it.value.preview }

    // 歌单集
    private val playlists: PlaylistMap = mutableMapOf()
    // 所有歌单名称
    val playlistNames: List<String> get() = playlists.map { it.key }
    // 查找歌单
    fun findPlaylist(name: String): Playlist? = playlists[name]
    // 指定歌单预览
    fun previewPlaylist(name: String): PlaylistPreview {
        val playlist = playlists[name]
        return PlaylistPreview(name, playlist?.items?.map {
            val info = musicInfos[it]
            PlaylistPreview.MusicItem(it, info?.name ?: it, info?.singer ?: "", info == null)
        } ?: emptyList())
    }

    // 当前播放歌单
    var currentPlaylist: Playlist? = null
    inline fun withPlaylist(block: (Playlist) -> Unit) = currentPlaylist?.let(block)

    // 当前播放音乐
    val currentMusicInfo: MusicInfo? get() = musicInfos[player.currentMediaItem?.mediaId]
    inline fun withMusic(block: (MusicInfo) -> Unit) = currentMusicInfo?.let(block)

    // 定位播放媒体
    fun indexOfMusic(id: String): Int {
        val timeline = player.currentTimeline
        val window = Timeline.Window()
        for (index in 0..< timeline.windowCount) {
            timeline.getWindow(index, window)
            if (window.mediaItem.mediaId == id) return index
        }
        return -1
    }

    // 当前播放媒体列表预览
    val previewCurrentPlaylist: PlayingMusicPreviewList get() {
        val timeline = player.currentTimeline
        val window = Timeline.Window()
        val items = mutableListOf<PlayingMusicPreview>()
        val currentMusic = currentMusicInfo
        for (index in 0..< timeline.windowCount) {
            timeline.getWindow(index, window)
            val musicInfo = musicInfos[window.mediaItem.mediaId]
            musicInfo?.let { items += PlayingMusicPreview(it.id, it.name, it.singer, it == currentMusic) }
        }
        return items
    }

    // 生成播放媒体
    private val MusicInfo.buildMusicItem: MediaItem get() = MediaItem.Builder()
        .setMediaId(this.id)
        .setUri(Uri.fromFile(this.audioPath))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.name)
                .setArtist(this.singer)
                .setAlbumTitle(this.album)
                .setAlbumArtist(this.singer)
                .setComposer(this.composer)
                .setWriter(this.lyricist)
                .setArtworkUri(FileProvider.getUriForFile(context, context.rs(R.string.app_provider), this.recordPath))
                .build())
        .build()

    // 播放器更新回调
    private val onTimeUpdate = object : Runnable {
        override fun run() {
            uiListener.onMusicUpdate(player.currentPosition)
            handler.postDelayed(this, UPDATE_FREQUENCY) // 更新消息
        }
    }

    // 准备播放器
    @IOThread
    fun preparePlayer(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        player = mediaControllerFuture.get()
        player.addListener(this)

        pathMusic.listFiles { file -> file.isFile && file.name.lowercase().endsWith(MusicRes.INFO_NAME) }?.let {
            for (file in it) {
                try {
                    val info: MusicInfo = file.readJson()
                    if (info.isCorrect) musicInfos[info.id] = info
                }
                catch (_: Exception) { }
            }
        }

        playlists.putAll(Config.playlist)
    }

    // 释放播放器
    fun release() {
        handler.removeCallbacks(onTimeUpdate)
        player.removeListener(this)
    }

    // 更新播放模式
    fun updatePlayMode() {
        when (Config.music_play_mode) {
            MusicPlayMode.ORDER -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            MusicPlayMode.LOOP -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            MusicPlayMode.RANDOM -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
            }
        }
    }

    // 切换播放模式
    fun nextPlayMode() {
        val nextMode = when (val mode = Config.music_play_mode) {
            MusicPlayMode.ORDER -> MusicPlayMode.LOOP
            MusicPlayMode.LOOP -> MusicPlayMode.RANDOM
            MusicPlayMode.RANDOM -> MusicPlayMode.ORDER
            else -> mode
        }
        Config.music_play_mode = nextMode
        updatePlayMode()
    }

    // 恢复上一次播放
    fun resumeLastMusic() {
        val playlistName = Config.music_last_playlist
        val musicId = Config.music_last_music
        playlists[playlistName]?.let { playlist ->
            val mediaItems = mutableListOf<MediaItem>()
            for (id in playlist.items) musicInfos[id]?.let { mediaItems += it.buildMusicItem }
            currentPlaylist = playlist // 设置当前播放歌单
            // 暂停 Player
            val musicIndex = mediaItems.indexOfFirst { it.mediaId == musicId }
            if (musicIndex == -1) player.setMediaItems(mediaItems, false)
            else player.setMediaItems(mediaItems, musicIndex, 0L)
            player.prepare()
            player.pause()
        }
    }

    // 创建歌单
    fun createPlaylist(name: String): Boolean {
        // 校验歌单
        if (name.isEmpty() || playlists.containsKey(name) ||
            name == context.rs(R.string.default_playlist_name)) return false
        // 创建一个空歌单
        playlists[name] = Playlist(name, mutableListOf())
        // 数据存储
        Config.playlist = playlists
        return true
    }

    // 重命名歌单
    fun renamePlaylist(oldName: String, newName: String): Boolean {
        val playlist = playlists[oldName] ?: return false
        // 校验歌单
        if (newName.isEmpty() || playlists.containsKey(newName) ||
            newName == context.rs(R.string.default_playlist_name)) return false
        // 重命名歌单
        playlists.remove(playlist.name)
        playlist.name = newName
        playlists[newName] = playlist
        // 数据存储
        Config.playlist = playlists
        return true
    }

    // 删除歌单
    fun deletePlaylist(name: String) {
        val playlist = playlists[name]
        if (playlist != null) {
            // 检查歌单是否正在播放
            if (playlist == currentPlaylist) stop()
            // UI更新
            playlists.remove(playlist.name)
            // 数据存储
            Config.playlist = playlists
        }
    }

    // 添加歌曲到歌单
    fun addMusicIntoPlaylist(playlist: Playlist, musicItems: MusicInfoPreviewList): Int {
        val oldItems = playlist.items
        val newItems = mutableListOf<String>()
        var num = 0
        for (selectItem in musicItems) {
            val id = selectItem.id
            if (!oldItems.contains(id)) { // 重复过滤
                ++num
                newItems += id
            }
        }
        if (num > 0) {
            // 添加到新列表中
            oldItems.addAll(newItems)
            // 数据存储
            Config.playlist = playlists
            // 检查当前是否在播放此列表
            if (playlist == currentPlaylist) {
                for (item in newItems) musicInfos[item]?.let { player.addMediaItem(it.buildMusicItem) }
            }
        }
        return num
    }

    // 从歌单中删除歌曲
    fun deleteMusicFromPlaylist(playlist: Playlist, id: String) {
        val position = playlist.items.indexOf(id)
        if (position != -1) {
            // 检查歌单是否正在播放
            if (playlist == currentPlaylist) {
                // 移除正在播放的媒体
                val mediaIndex = indexOfMusic(id)
                if (mediaIndex != -1) player.removeMediaItem(mediaIndex)
            }
            // 从歌单中移除
            playlist.items.removeAt(position)
            // 数据存储
            Config.playlist = playlists
        }
    }

    // 在歌单中移动歌曲
    fun moveMusicInPlaylist(playlist: Playlist, oldPosition: Int, newPosition: Int) {
        // 检查歌单是否正在播放
        val items = playlist.items
        if (playlist == currentPlaylist) {
            // 记正常音频是 N , 删除音频是 D
            // 1. 移动起点是 N
            //      1.1 移动路径上没有 N 则播放媒体次序不变
            //      1.2 移动路径上有 N
            //          1.2.1 移动终点是 N 则移动媒体次序
            //          1.2.2 移动终点是 D 且为前移 则找 D 后面第一个 N 移动媒体次序
            //          1.2.3 移动终点是 D 且为后移 则找 D 前面第一个 N 移动媒体次序
            // 2. 移动起点是 D 则播放媒体次序不变
            val startInfo = musicInfos[items[oldPosition]]
            if (startInfo != null) {
                var endInfo: MusicInfo? = null
                val range = if (oldPosition < newPosition) newPosition downTo oldPosition + 1 else newPosition..< oldPosition
                for (index in range) {
                    val info = musicInfos[items[index]]
                    if (info != null) {
                        endInfo = info
                        break
                    }
                }
                if (endInfo != null) {
                    val startMediaIndex = indexOfMusic(startInfo.id)
                    val endMediaIndex = indexOfMusic(endInfo.id)
                    if (startMediaIndex != -1 && endMediaIndex != -1) {
                        player.moveMediaItem(startMediaIndex, endMediaIndex)
                    }
                }
            }
        }
        // 移动歌单两个歌曲的位置
        items.moveItem(oldPosition, newPosition)
        // 数据存储
        Config.playlist = playlists
    }

    // 删除歌曲
    suspend fun deleteMusic(musicItems: MusicInfoPreviewList) {
        // 当前正在播放
        if (currentPlaylist != null) {
            // 检查播放列表是否存在需要被删除的音乐
            for (selectItem in musicItems) {
                val index = indexOfMusic(selectItem.id)
                if (index != -1) player.removeMediaItem(index)
            }
        }
        // 删除所有音乐信息
        for (selectItem in musicItems) musicInfos.remove(selectItem.id)
        // 删除本地文件
        withContext(Dispatchers.IO) {
            for (selectItem in musicItems) pathMusic.deleteFilter(selectItem.id)
        }
    }

    // 播放歌单
    fun start(playlist: Playlist) {
        val mediaItems = mutableListOf<MediaItem>()
        for (id in playlist.items) musicInfos[id]?.let { mediaItems += it.buildMusicItem }
        currentPlaylist = playlist // 设置当前播放歌单
        // 启动 Player
        player.setMediaItems(mediaItems, false)
        player.prepare()
        player.play()
    }

    // 暂停
    fun pause() {
        if (player.isPlaying) player.pause()
    }

    // 停止
    fun stop() {
        player.clearMediaItems()
        player.stop()
    }

    // 设置进度百分比
    fun setProgressPercent(percent: Float) {
        if (currentMusicInfo != null) {
            player.seekTo((player.duration * percent).toLong())
            if (!player.isPlaying) player.play()
        }
    }

    // 播放或暂停
    fun playOrPause() {
        if (currentMusicInfo != null) {
            if (player.isPlaying) player.pause()
            else player.play()
        }
    }

    // 切换上一首
    fun gotoPrevious() {
        val index = player.previousMediaItemIndex
        if (index != -1) {
            player.seekTo(index, 0)
            if (!player.isPlaying) player.play()
        }
    }

    // 切换下一首
    fun gotoNext() {
        val index = player.nextMediaItemIndex
        if (index != -1) {
            player.seekTo(index, 0)
            if (!player.isPlaying) player.play()
        }
    }

    // 跳转指定位置
    fun gotoIndex(index: Int) {
        if (index != -1 && player.currentMediaItemIndex != index) {
            player.seekTo(index, 0L)
            if (!player.isPlaying) player.play()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        uiListener.onMusicModeChanged(Config.music_play_mode)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        uiListener.onMusicModeChanged(Config.music_play_mode)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        println("onMediaItemTransition mediaItem=${mediaItem?.mediaId} reason=$reason")
        val musicInfo = musicInfos[mediaItem?.mediaId]

        // 更新上次播放记录
        Config.music_last_playlist = currentPlaylist?.name ?: ""
        Config.music_last_music = musicInfo?.id ?: ""

        uiListener.onMusicChanged(musicInfo)
        val duration = player.duration
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && musicInfo != null && duration != C.TIME_UNSET) {
            uiListener.onMusicReady(musicInfo, player.currentPosition, duration)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        println("onPlayerError error=$error")
        stop()
        uiListener.onMusicError(error)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> println("onPlaybackStateChanged playbackState=IDLE")
            Player.STATE_BUFFERING -> println("onPlaybackStateChanged playbackState=BUFFERING")
            Player.STATE_READY -> {
                println("onPlaybackStateChanged playbackState=READY isPlaying=${player.isPlaying}")
                val musicInfo = musicInfos[player.currentMediaItem?.mediaId]
                val duration = player.duration
                if (musicInfo != null && duration != C.TIME_UNSET) {
                    uiListener.onMusicReady(musicInfo, player.currentPosition, duration)
                }
            }
            Player.STATE_ENDED -> {
                println("onPlaybackStateChanged playbackState=ENDED ${player.mediaItemCount}")
                if (player.mediaItemCount == 0) {
                    currentPlaylist = null
                    uiListener.onMusicStop()
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        println("onIsPlayingChanged isPlaying=$isPlaying")
        handler.removeCallbacks(onTimeUpdate)
        if (isPlaying) handler.post(onTimeUpdate)
        uiListener.onMusicPlaying(isPlaying)
    }
}