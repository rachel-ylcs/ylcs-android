package com.yinlin.rachel.view

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.backgroundColor
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.data.music.LyricsSettings
import com.yinlin.rachel.tool.rd
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.textSizePx

class FloatingLyricsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {
    companion object {
        const val DEFAULT_HEIGHT_RATIO = 150
    }

    val guideline1 = Guideline(context).apply {
        id = generateViewId()
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            guidePercent = 0f
            orientation = 1
        }
    }
    val guideline2 = Guideline(context).apply {
        id = generateViewId()
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            guidePercent = 1f
            orientation = 1
        }
    }
    private val surface = TextView(context, null, R.style.RachelText_X0_Overflow).apply {
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        text = context.rs(R.string.no_lyrics)
        paint.isFakeBoldText = true
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
            topToTop = 0
            bottomToBottom = 0
            leftToRight = guideline1.id
            rightToLeft = guideline2.id
        }
    }

    private var lrcData: LrcData? = null
    private var currentIndex: Int = -1

    val isAttached: Boolean get() = this.isAttachedToWindow
    val canShow: Boolean get() = Settings.canDrawOverlays(context)

    var showState: Boolean = false

    init {
        addView(guideline1)
        addView(guideline2)
        addView(surface)
    }

    fun updateSettings(settings: LyricsSettings): FloatingLyricsView {
        surface.apply {
            textSizePx = settings.textSize * context.rd(R.dimen.sm)
            textColor = settings.textColor
            backgroundColor = settings.backgroundColor
        }
        setPadding(0, (settings.offsetY * DEFAULT_HEIGHT_RATIO).toInt(), 0, 0)
        guideline1.setGuidelinePercent(settings.offsetLeft)
        guideline2.setGuidelinePercent(settings.offsetRight)
        return this
    }

    fun load(data: LrcData?) {
        lrcData = data
        currentIndex = -1
        surface.text = ""
    }

    fun clear() {
        lrcData = null
        currentIndex = -1
        surface.text = ""
    }

    fun update(position: Long) {
        if (currentIndex >= 2) {
            lrcData?.data?.let { surface.text = it[currentIndex].text }
        }
    }

    fun needUpdate(position: Long): Boolean {
        if (!showState) return false
        lrcData?.data?.let {
            val index = findIndex(it, position)
            if (index != currentIndex) {
                currentIndex = index
                return true
            }
        }
        return false
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