package com.yinlin.rachel.model.engine

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.view.TextureView
import com.yinlin.rachel.R
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.readText
import com.yinlin.rachel.tool.rf
import com.yinlin.rachel.tool.textHeight
import com.yinlin.rachel.tool.toDP
import java.io.File


// 多行文本歌词引擎
class LineLyricsEngine(context: Context) : LyricsEngine {
    class TextPaints(context: Context) {
        private var canvasWidth: Float = 512f
        private var canvasHeight: Float = 512f
        private val maxTextHeight: Float = 30f.toDP(context)

        private val main = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.rc(R.color.music_lyrics_main)
            textAlign = Paint.Align.CENTER
            typeface = context.rf(R.font.xwwk)
            isFakeBoldText = true
            setShadowLayer(2f, 2f, 2f, context.rc(R.color.dark))
        }

        private val second = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.rc(R.color.music_lyrics_second)
            textAlign = Paint.Align.CENTER
            typeface = context.rf(R.font.xwwk)
            setShadowLayer(1f, 1f, 1f, context.rc(R.color.dark))
        }

        private val fade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.rc(R.color.music_lyrics_fade)
            textAlign = Paint.Align.CENTER
            typeface = context.rf(R.font.xwwk)
        }

        fun adjustPaint(maxWidth: Float, maxHeight: Float, maxLengthText: String) {
            canvasWidth = maxWidth
            canvasHeight = maxHeight
            val availableWidth = maxWidth * MAX_WIDTH_RATIO
            main.textSize = 18f
            var detectWidth = main.measureText(maxLengthText)
            while (detectWidth < availableWidth) {
                main.textSize += 5f
                if (main.textHeight > maxTextHeight) break
                detectWidth = main.measureText(maxLengthText)
            }
            second.textSize = main.textSize * 0.85f
            fade.textSize = main.textSize * 0.8f
        }

        fun draw(canvas: Canvas, str1: String, str2: String, str3: String, str4: String, str5: String) {
            val x = canvasWidth / 2f
            val y = canvasHeight / 2f
            val gap = maxTextHeight * 1.2f

            canvas.apply {
                drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
                drawText(str3, x, y, main)
                drawText(str2, x, y - gap, second)
                drawText(str4, x, y + gap, second)
                drawText(str1, x, y - gap * 2, fade)
                drawText(str5, x, y + gap * 2, fade)
            }
        }
    }

    companion object {
        const val MAX_WIDTH_RATIO = 0.8f
        const val DEFAULT_RES = ""
        const val NAME = "line"
        const val DESCRIPTION = "原生逐行歌词渲染引擎, 自适应字体大小, 自内向外渐隐"
        val ICON: Int = R.drawable.img_lyrics_engine_line
    }
    override val name: String = NAME
    override val ext: String = ".lrc"

    private val paints = TextPaints(context)
    private var currentIndex: Int = -1
    private var texture: TextureView? = null
    private var lrcData: LrcData? = null

    override fun load(texture: TextureView, surface: SurfaceTexture, width: Int, height: Int, file: String): Boolean {
        // 检查是否已经解析过音频的LRC歌词
        lrcData = null
        this.texture = texture
        currentIndex = -1
        try {
            lrcData = LrcData.parseLrcData(File(file).readText())
            paints.adjustPaint(texture.measuredWidth.toFloat(), texture.measuredHeight.toFloat(), lrcData!!.maxLengthText)
            return true
        }
        catch (_: Exception) { clear() }
        return false
    }

    override fun clear() {
        lrcData = null
        texture = null
        currentIndex = -1
    }

    override fun release() = clear()

    override fun needUpdate(position: Long): Boolean {
        lrcData?.data?.let {
            val index = findIndex(it, position)
            if (index != currentIndex) {
                currentIndex = index
                return true
            }
        }
        return false
    }

    override fun update(position: Long) {
        if (currentIndex >= 2) {
            texture?.let {
                val canvas = it.lockCanvas()
                val items = lrcData?.data
                if (canvas != null && items != null) {
                    val item1 = items[currentIndex - 2]
                    val item2 = items[currentIndex - 1]
                    val item3 = items[currentIndex]
                    val item4 = items[currentIndex + 1]
                    val item5 = items[currentIndex + 2]
                    paints.draw(canvas, item1.text, item2.text, item3.text, item4.text, item5.text)
                    it.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    private fun searchIndex(items: List<LrcData.LineItem>, position: Long): Int {
        var index = 0
        val len = items.size - 1
        while (index < len) {
            val currentItem = items[index]
            val nextItem = items[index + 1]
            if (currentItem.position <= position && nextItem.position > position) return index
            ++index
        }
        return 2
    }

    private fun findIndex(items: List<LrcData.LineItem>, position: Long): Int {
        if (currentIndex == -1) return 2
        else {
            val currentItem = items[currentIndex]
            val nextItem = items[currentIndex + 1]
            return if (currentItem.position <= position && nextItem.position > position) currentIndex else searchIndex(items, position)
        }
    }
}