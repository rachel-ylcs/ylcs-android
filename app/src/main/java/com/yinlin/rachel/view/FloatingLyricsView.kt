package com.yinlin.rachel.view

import android.content.Context
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.yinlin.rachel.backgroundColor
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.data.music.LyricsSettings
import com.yinlin.rachel.databinding.FloatingLyricsBinding
import com.yinlin.rachel.textColor

class FloatingLyricsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private val v: FloatingLyricsBinding = FloatingLyricsBinding.inflate(LayoutInflater.from(context), this, true)

    private var lrcData: LrcData? = null
    private var currentIndex: Int = -1

    val isAttached: Boolean get() = this.isAttachedToWindow
    val canShow: Boolean get() = Settings.canDrawOverlays(context)

    var showState: Boolean = false

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun load(data: LrcData?) {
        lrcData = data
        currentIndex = -1
        v.surface.text = ""
    }

    fun clear() {
        lrcData = null
        currentIndex = -1
        v.surface.text = ""
    }

    fun update(position: Long) {
        if (currentIndex >= 2) {
            lrcData?.data?.let { v.surface.text = it[currentIndex].text }
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

    fun updateSettings(settings: LyricsSettings): FloatingLyricsView {
        v.surface.apply {
            textColor = settings.textColor
            backgroundColor = settings.backgroundColor
        }
        setOffsetY(settings.offsetY)
        v.placeholder1.setWidthPercent(settings.offsetX)
        v.surface.setWidthPercent(settings.width)
        return this
    }

    private fun View.setWidthPercent(value: Float) {
        layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintPercentWidth = value
        }
    }

    private fun setOffsetY(value: Float) {
        v.root.setPadding(0, (value * 150).toInt(), 0, 0)
    }
}