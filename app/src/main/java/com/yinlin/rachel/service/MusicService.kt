package com.yinlin.rachel.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.common.buildFfmpegPlayer

class MusicService : MediaLibraryService(), MediaLibraryService.MediaLibrarySession.Callback {
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        player = buildFfmpegPlayer(this)
        session = MediaLibrarySession.Builder(this, player, this)
            .setSessionActivity(prepareSessionActivity())
            .build()
    }

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    private fun prepareSessionActivity(): PendingIntent {
        val intent = Intent()
        intent.setComponent(ComponentName(this@MusicService, MainActivity::class.java))
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
        return PendingIntent.getActivity(this, 0, intent, flags)
    }
}