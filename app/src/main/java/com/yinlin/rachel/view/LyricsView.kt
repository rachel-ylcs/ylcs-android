package com.yinlin.rachel.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.tool.div
import com.yinlin.rachel.model.engine.LyricsEngine
import com.yinlin.rachel.model.engine.LyricsEngineFactory
import com.yinlin.rachel.tool.pathMusic

class LyricsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {
    private var lyricsEngine: LyricsEngine? = null
    private var lyricsFileName: String? = null
    private var musicInfo: MusicInfo? = null
    private var isEngineLoad: Boolean = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val engine = lyricsEngine
        val filename = lyricsFileName
        if (engine != null && filename != null && musicInfo != null && childCount != 0) {
            if (engine.load(getChildAt(0) as TextureView, surface, width, height, filename)) {
                isEngineLoad = true
                update(0)
            }
            else releaseEngine()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) { }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) { }

    private fun addTexture() {
        if (childCount != 0) removeAllViews()
        addView(TextureView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            isOpaque = false
            surfaceTextureListener = this@LyricsView
        })
    }

    // 加载歌词引擎
    @IOThread
    fun loadEngine(musicInfo: MusicInfo): Boolean {
        val lyrics = musicInfo.lyrics
        // 先检查当前歌词引擎是否合适
        if (lyricsEngine != null) {
            // 检查歌曲是否含有当前歌词引擎
            if (lyrics.containsKey(lyricsEngine?.name)) lyricsEngine?.clear()
            else releaseEngine()
        }
        if (lyricsEngine == null) {
            // 寻找可以使用的歌词引擎
            for ((engineName, _) in lyrics) {
                if (LyricsEngineFactory.hasEngine(engineName)) {
                    lyricsEngine = LyricsEngineFactory.newEngine(context, engineName)
                    break
                }
            }
        }
        lyricsEngine?.let {
            val firstLyrics = lyrics[it.name]?.first() ?: ""
            lyricsFileName = (pathMusic / "${musicInfo.id}${if(firstLyrics.isEmpty()) "" else "_"}${firstLyrics}${it.ext}").absolutePath
            this.musicInfo = musicInfo
            addTexture()
            return true
        }
        return false
    }

    // 切换歌词引擎
    @IOThread
    fun switchEngine(musicInfo: MusicInfo, engineName: String, name: String): Boolean {
        // 先检查当前歌词引擎是否合适
        if (lyricsEngine != null) {
            // 检查是否需要更换引擎
            if (lyricsEngine?.name != engineName) releaseEngine()
            else lyricsEngine?.clear()
        }
        if (lyricsEngine == null) lyricsEngine = LyricsEngineFactory.newEngine(context, engineName)
        if (lyricsEngine == null) return false
        else {
            val filename = if (name.isEmpty()) "" else "_${name}"
            lyricsFileName = (pathMusic / "${musicInfo.id}${filename}${lyricsEngine?.ext}").absolutePath
            this.musicInfo = musicInfo
            addTexture()
            return true
        }
    }

    fun releaseEngine() {
        if (lyricsEngine != null) {
            lyricsEngine?.release()
            lyricsEngine = null
            lyricsFileName = null
            musicInfo = null
            isEngineLoad = false
            removeAllViews()
        }
    }

    fun update(position: Long) {
        if (isEngineLoad && lyricsEngine?.needUpdate(position) == true) lyricsEngine?.update(position)
    }
}