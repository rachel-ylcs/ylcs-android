package com.yinlin.rachel.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.yinlin.rachel.tool.buildFfmpegPlayer

@OptIn(UnstableApi::class)
class MusicService : MediaLibraryService(), MediaLibraryService.MediaLibrarySession.Callback {
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession

    override fun onCreate() {
        super.onCreate()
        player = buildFfmpegPlayer(this)
        session = MediaLibrarySession.Builder(this, player, this).build()
    }

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
}