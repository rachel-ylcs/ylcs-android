package com.yinlin.rachel.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.buildFfmpegPlayer

@OptIn(UnstableApi::class)
class MusicService : MediaLibraryService(), MediaLibraryService.MediaLibrarySession.Callback {
    companion object {
        const val CHANNEL_ID = "rachel_music"
        const val NOTIFICATION_ID = 1211
    }

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var notificationManager: PlayerNotificationManager

    override fun onCreate() {
        super.onCreate()
        player = buildFfmpegPlayer(this)
        session = MediaLibrarySession.Builder(this, player, this)
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent().apply {
                setComponent(ComponentName(this@MusicService, MainActivity::class.java))
            }, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT))
            .build()
    }

    override fun onDestroy() {
        session.release()
        notificationManager.setPlayer(null)
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
}