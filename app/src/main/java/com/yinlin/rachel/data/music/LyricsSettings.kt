package com.yinlin.rachel.data.music

import android.graphics.Color
import androidx.annotation.ColorInt

data class LyricsSettings(
    // 左侧偏移 0.0 ~ 1.0
    var offsetLeft: Float = 0f,
    // 右侧偏移 0.0 ~ 1.0
    var offsetRight: Float = 1f,
    // 纵向偏移 0.0 ~ 2.0
    var offsetY: Float = 1f,
    // 字体大小 0.75 ~ 1.5
    var textSize: Float = 1f,
    // 字体颜色
    @ColorInt var textColor: Int = Color.rgb(70, 130, 180),
    // 背景颜色
    @ColorInt var backgroundColor: Int = 0
)