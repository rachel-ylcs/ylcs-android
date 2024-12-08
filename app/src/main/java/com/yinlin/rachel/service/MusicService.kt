package com.yinlin.rachel.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.common.buildFfmpegPlayer
import com.yinlin.rachel.data.music.Command
import com.yinlin.rachel.data.music.MusicPlayMode
import com.yinlin.rachel.tool.Config
import kotlin.random.Random

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService(), MediaSession.Callback {
    class ForwardPlayer(private val player: ExoPlayer, mode: MusicPlayMode) : ForwardingPlayer(player) {
        init {
            player.updatePlayMode(mode)
        }

        override fun getAvailableCommands() = prepareNotificationPlayerCommands
        override fun seekToPreviousMediaItem() = gotoPrevious()
        override fun seekToPrevious() = gotoPrevious()
        override fun seekToNextMediaItem() = gotoNext()
        override fun seekToNext() = gotoNext()
        private fun gotoPrevious() {
            val index = player.previousMediaItemIndex
            if (index != -1) {
                player.seekTo(index, 0)
                if (!player.isPlaying) player.play()
            }
            else if (player.repeatMode == Player.REPEAT_MODE_ONE && player.currentMediaItemIndex != -1) { // 单曲循环
                player.seekTo(player.mediaItemCount - 1, 0)
                if (!player.isPlaying) player.play()
            }
        }
        private fun gotoNext() {
            val index = player.nextMediaItemIndex
            if (index != -1) {
                player.seekTo(index, 0)
                if (!player.isPlaying) player.play()
            }
            else if (player.repeatMode == Player.REPEAT_MODE_ONE && player.currentMediaItemIndex != -1) { // 单曲循环
                player.seekTo(0, 0L)
                if (!player.isPlaying) player.play()
            }
        }
    }

    companion object {
        private val prepareNotificationPlayerCommands get() = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
            .build()

        private val prepareSessionCommands get() = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(Command.CommandPlayOrPause)
            .add(Command.CommandPause)
            .add(Command.CommandStop)
            .add(Command.CommandGotoIndex)
            .add(Command.CommandGetMode)
            .add(Command.CommandNextMode)
            .add(Command.CommandSetProgressPercent)
            .add(Command.CommandShuffle)
            .build()

        private fun Player.updatePlayMode(mode: MusicPlayMode) {
            when (mode) {
                MusicPlayMode.ORDER -> {
                    repeatMode = Player.REPEAT_MODE_ALL
                    shuffleModeEnabled = false
                }
                MusicPlayMode.LOOP -> {
                    repeatMode = Player.REPEAT_MODE_ONE
                    shuffleModeEnabled = false
                }
                MusicPlayMode.RANDOM -> {
                    repeatMode = Player.REPEAT_MODE_ALL
                    shuffleModeEnabled = true
                }
            }
        }

        private fun Player.createShuffledList(current: Int): ShuffleOrder {
            val seed = System.currentTimeMillis()
            val random = Random(seed)
            val length = mediaItemCount
            val shuffled = IntArray(length)
            var swapIndex: Int
            for (i in 0 ..< length) {
                swapIndex = random.nextInt(i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i
            }
            swapIndex = shuffled.indexOf(current)
            shuffled[swapIndex] = shuffled[0]
            shuffled[0] = current
            return DefaultShuffleOrder(shuffled, seed)
        }
    }

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession

    private val buttonOrderMode = CommandButton.Builder()
        .setDisplayName("顺序播放")
        .setIconResId(R.drawable.icon_play_mode_order)
        .setSessionCommand(Command.CommandNextMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()
    private val buttonLoopMode = CommandButton.Builder()
        .setDisplayName("单曲循环")
        .setIconResId(R.drawable.icon_play_mode_loop)
        .setSessionCommand(Command.CommandNextMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()
    private val buttonRandomMode = CommandButton.Builder()
        .setDisplayName("随机播放")
        .setIconResId(R.drawable.icon_player_mode_random)
        .setSessionCommand(Command.CommandNextMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()
    private val buttonStop = CommandButton.Builder()
        .setDisplayName("停止播放")
        .setIconResId(R.drawable.icon_stop)
        .setSessionCommand(Command.CommandStop)
        .setSlots(CommandButton.SLOT_BACK_SECONDARY)
        .build()

    override fun onCreate() {
        super.onCreate()
        player = buildFfmpegPlayer(this)
        val mode = Config.music_play_mode
        session = MediaSession.Builder(this, ForwardPlayer(player, mode))
            .setCallback(this)
            .setMediaButtonPreferences(prepareButtons(mode))
            .setSessionActivity(prepareSessionActivity())
            .build()
    }

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
        val builder = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
        builder.setAvailablePlayerCommands(prepareNotificationPlayerCommands)
        builder.setAvailableSessionCommands(prepareSessionCommands)
        return builder.build()
    }

    override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> = when (customCommand) {
        Command.CommandPlayOrPause -> {
            if (player.currentMediaItem != null) {
                if (player.isPlaying) player.pause()
                else player.play()
            }
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        Command.CommandPause -> {
            if (player.isPlaying) player.pause()
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        Command.CommandStop -> {
            player.clearMediaItems()
            player.stop()
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        Command.CommandGotoIndex -> {
            val index = args.getInt(Command.ARG_INDEX, -1)
            if (index != -1 && player.currentMediaItemIndex != index) {
                player.seekTo(index, 0L)
                if (!player.isPlaying) player.play()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            else Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
        }
        Command.CommandGetMode -> {
            val ret = Bundle().apply { putInt(Command.ARG_MODE, Config.music_play_mode.ordinal) }
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, ret))
        }
        Command.CommandNextMode -> {
            val nextMode = when (val mode = Config.music_play_mode) {
                MusicPlayMode.ORDER -> MusicPlayMode.LOOP
                MusicPlayMode.LOOP -> MusicPlayMode.RANDOM
                MusicPlayMode.RANDOM -> MusicPlayMode.ORDER
                else -> mode
            }
            session.setMediaButtonPreferences(prepareButtons(nextMode))
            player.updatePlayMode(nextMode)
            Config.music_play_mode = nextMode
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        Command.CommandSetProgressPercent -> {
            val percent = args.getFloat(Command.ARG_PROGRESS_PERCENT, -1f)
            if (percent in 0f..1f) {
                if (player.currentMediaItem != null) {
                    player.seekTo((player.duration * percent).toLong())
                    if (!player.isPlaying) player.play()
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            else Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
        }
        Command.CommandShuffle -> {
            if (player.shuffleModeEnabled) {
                val current = player.currentMediaItemIndex
                if (current != -1) {
                    val timeline = player.currentTimeline
                    var windowIndex = timeline.getFirstWindowIndex(true)
                    var count = 0
                    while (windowIndex != C.INDEX_UNSET) {
                        if (windowIndex == current) break
                        windowIndex = timeline.getNextWindowIndex(windowIndex, Player.REPEAT_MODE_OFF,true)
                        count++
                    }
                    if (windowIndex != -1 && count == 0) player.setShuffleOrder(player.createShuffledList(current))
                }
            }
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        else -> super.onCustomCommand(session, controller, customCommand, args)
    }

    private fun prepareButtons(mode: MusicPlayMode): List<CommandButton> {
        val buttons = mutableListOf<CommandButton>()
        when (mode) {
            MusicPlayMode.ORDER -> buttons += buttonOrderMode
            MusicPlayMode.LOOP -> buttons += buttonLoopMode
            MusicPlayMode.RANDOM -> buttons += buttonRandomMode
        }
        buttons += buttonStop
        return buttons
    }

    private fun prepareSessionActivity(): PendingIntent {
        val intent = Intent()
        intent.setComponent(ComponentName(this@MusicService, MainActivity::class.java))
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
        return PendingIntent.getActivity(this, 0, intent, flags)
    }
}