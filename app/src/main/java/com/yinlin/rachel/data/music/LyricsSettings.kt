package com.yinlin.rachel.data.music

import android.graphics.Color
import androidx.annotation.ColorInt

data class LyricsSettings(
    @ColorInt var textColor: Int = Color.rgb(70, 130, 180),
    @ColorInt var backgroundColor: Int = Color.argb(128, 255, 160, 122),
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var width: Float = 1f,
)