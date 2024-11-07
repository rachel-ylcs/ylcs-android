package com.yinlin.rachel.data.activity

import com.haibin.calendarview.Calendar
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

open class ShowActivityPreview(
    val ts: String,
    val title: String,
) {
    val calendar: Calendar? get() = try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(ts)
        val zonedDateTime = ZonedDateTime.ofInstant(d!!.toInstant(), ZoneId.systemDefault())
        Calendar().apply {
            year = zonedDateTime.year
            month = zonedDateTime.monthValue
            day = zonedDateTime.dayOfMonth
            scheme = title
        }
    }
    catch (ignored: Exception) { null }
}

typealias ShowActivityPreviewList = List<ShowActivityPreview>