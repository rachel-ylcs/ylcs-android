package com.yinlin.rachel.tool

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.yinlin.rachel.Config

@OptIn(UnstableApi::class)
class FfmpegRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    init { setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER) }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        out += FfmpegAudioRenderer()
        super.buildAudioRenderers(context, extensionRendererMode, mediaCodecSelector,
            enableDecoderFallback, audioSink, eventHandler, eventListener, out)
    }
}

@OptIn(UnstableApi::class)
fun buildFfmpegPlayer(context: Context) = ExoPlayer.Builder(context)
    .setAudioAttributes(AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build(), Config.music_focus)
    .setHandleAudioBecomingNoisy(true)
    .setRenderersFactory(FfmpegRenderersFactory(context))
    .build()