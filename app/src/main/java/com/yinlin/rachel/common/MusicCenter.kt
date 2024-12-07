package com.yinlin.rachel.common


import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.data.music.Command
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
import com.yinlin.rachel.tool.clearAddAll
import com.yinlin.rachel.tool.deleteFilterSafely
import com.yinlin.rachel.tool.div
import com.yinlin.rachel.tool.moveItem
import com.yinlin.rachel.tool.pathMusic
import com.yinlin.rachel.tool.readJson
import com.yinlin.rachel.tool.rs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


typealias RachelPlayer = MediaController

@OptIn(UnstableApi::class)
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
    fun previewPlaylist(playlist: Playlist) = PlaylistPreview(playlist.name, playlist.items.map {
        val info = musicInfos[it]
        PlaylistPreview.MusicItem(it, info?.name ?: it, info?.singer ?: "", info == null)
    })

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

    // 发送命令
    fun send(command: SessionCommand, args: Bundle = Bundle.EMPTY): Bundle =
        if (player.isConnected) player.sendCustomCommand(command, args).get().extras
        else Bundle.EMPTY

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
            player.clearMediaItems()
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
    fun renamePlaylist(playlist: Playlist, newName: String): Boolean {
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
    fun deletePlaylist(playlist: Playlist) {
        // 检查歌单是否正在播放
        if (playlist == currentPlaylist) send(Command.CommandStop)
        // UI更新
        playlists.remove(playlist.name)
        // 数据存储
        Config.playlist = playlists
    }

    // 重载歌单
    fun reloadPlaylist(newPlaylists: PlaylistMap) {
        // 更换歌单
        playlists.clearAddAll(newPlaylists)
        // 将当前播放列表脱离歌单
        currentPlaylist?.let { it.name = context.rs(R.string.default_playlist_name) }
        // 数据存储
        Config.playlist = playlists
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
            // 检查当前是否在播放此列表
            if (playlist == currentPlaylist) {
                for (item in newItems) musicInfos[item]?.let { player.addMediaItem(it.buildMusicItem) }
            }
            // 数据存储
            Config.playlist = playlists
        }
        return num
    }

    // 从歌单中删除歌曲
    fun deleteMusicFromPlaylist(playlist: Playlist, id: String) {
        val position = playlist.items.indexOf(id)
        if (position != -1) {
            // 从歌单中移除
            playlist.items.removeAt(position)
            // 检查歌单是否正在播放
            if (playlist == currentPlaylist) {
                // 移除正在播放的媒体
                val mediaIndex = indexOfMusic(id)
                if (mediaIndex != -1) player.removeMediaItem(mediaIndex)
            }
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
        // 移动歌单歌曲的位置
        items.moveItem(oldPosition, newPosition)
        // 数据存储
        Config.playlist = playlists
    }

    // 提醒歌曲已添加
    suspend fun notifyAddMusic(ids: List<String>) {
        // 添加前必须停止播放器
        // 1. 保证了导入时能够覆盖正在播放的音频文件
        // 2. 无需考虑更新恢复被删除的歌单中的歌曲以及更新新版本的歌曲与元数据
        val addInfos = mutableListOf<MusicInfo>()
        val removeInfos = mutableListOf<MusicInfo>()
        withContext(Dispatchers.IO) {
            for (id in ids) {
                val info: MusicInfo = (pathMusic / (id + MusicRes.INFO_NAME)).readJson()
                if (info.isCorrect) addInfos += info
                else removeInfos += info
            }
        }
        for (info in addInfos) musicInfos[info.id] = info
        for (info in removeInfos) musicInfos.remove(info.id)
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
            for (selectItem in musicItems) pathMusic.deleteFilterSafely(selectItem.id)
        }
    }

    // 获取播放模式
    val playMode: MusicPlayMode get() {
        val mode = send(Command.CommandGetMode).getInt(Command.ARG_MODE, -1)
        return if (mode == -1) MusicPlayMode.ORDER else MusicPlayMode(mode)
    }

    // 播放歌单
    fun start(playlist: Playlist) {
        if (playlist != currentPlaylist) {
            val mediaItems = mutableListOf<MediaItem>()
            for (id in playlist.items) musicInfos[id]?.let { mediaItems += it.buildMusicItem }
            if (mediaItems.isNotEmpty()) {
                currentPlaylist = playlist // 设置当前播放歌单
                // 启动 Player
                player.clearMediaItems()
                player.setMediaItems(mediaItems, false)
                player.prepare()
                player.play()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        uiListener.onMusicModeChanged(playMode)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        uiListener.onMusicModeChanged(playMode)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        val musicInfo = musicInfos[mediaItem?.mediaId]

        // 随机列表洗牌
        send(Command.CommandShuffle)

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
        send(Command.CommandStop)
        uiListener.onMusicError(error)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> {
                currentPlaylist = null
                uiListener.onMusicStop()
            }
            Player.STATE_BUFFERING -> { }
            Player.STATE_READY -> {
                val musicInfo = musicInfos[player.currentMediaItem?.mediaId]
                val duration = player.duration
                if (musicInfo != null && duration != C.TIME_UNSET) {
                    uiListener.onMusicReady(musicInfo, player.currentPosition, duration)
                }
            }
            Player.STATE_ENDED -> {
                if (player.mediaItemCount == 0) send(Command.CommandStop)
                else if (!player.isPlaying) player.play() // 单曲循环
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        handler.removeCallbacks(onTimeUpdate)
        if (isPlaying) handler.post(onTimeUpdate)
        uiListener.onMusicPlaying(isPlaying)
    }
}