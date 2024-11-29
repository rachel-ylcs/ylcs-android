package com.yinlin.rachel.tool

import android.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun compareLatestTime(t1: String, t2: String): Boolean = try {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    LocalDateTime.parse(t1, formatter).isAfter(LocalDateTime.parse(t2, formatter))
} catch (_: Exception) { false }

val Long.timeString: String get() {
    val hours = (this / (1000 * 60 * 60)).toInt()
    val minutes = (this % (1000 * 60 * 60) / (1000 * 60)).toInt()
    val seconds = (this % (1000 * 60) / 1000).toInt()
    return if (hours > 0) String.format(Locale.SIMPLIFIED_CHINESE,"%02d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.SIMPLIFIED_CHINESE,"%02d:%02d", minutes, seconds)
}

val Long.timeStringWithHour: String get() {
    val hours = (this / (1000 * 60 * 60)).toInt()
    val minutes = (this % (1000 * 60 * 60) / (1000 * 60)).toInt()
    val seconds = (this % (1000 * 60) / 1000).toInt()
    return String.format(Locale.SIMPLIFIED_CHINESE,"%02d:%02d:%02d", hours, minutes, seconds)
}

val currentDateInteger: Int get() = try {
    val currentDate = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val formattedDate = currentDate.format(formatter)
    formattedDate.toInt()
}
catch (_: Exception) { System.currentTimeMillis().toInt() }

fun Int.attachAlpha(alpha: Int) = Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

fun Int.detachAlpha(): Int {
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)
    return Color.rgb(red, green, blue)
}

fun Int.getAlpha() = Color.alpha(this)