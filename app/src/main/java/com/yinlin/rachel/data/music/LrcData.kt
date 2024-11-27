package com.yinlin.rachel.data.music

import java.util.regex.Pattern

data class LrcData(
    val plainText: String,
    val data: List<LineItem>,
    val maxLengthText: String
) {
    data class LineItem(val position: Long, val text: String)

    companion object {
        fun parseLrcData(source: String): LrcData? = try {
            val items = mutableListOf<LineItem>()
            val plainText = StringBuilder()
            var maxLengthText = ""
            // 解析歌词文件
            val lines = source.split("\\r?\\n".toRegex())
            val pattern = Pattern.compile("\\[(\\d{2}):(\\d{2}).(\\d{2,3})](.*)")
            // 前三空行
            items += LineItem(-3, "")
            items += LineItem(-2, "")
            items += LineItem(-1, "")
            for (item in lines) {
                val line = item.trim()
                if (line.isEmpty()) continue
                val matcher = pattern.matcher(line)
                if (matcher.matches()) {
                    val minutes = matcher.group(1)!!.toLong()
                    val seconds = matcher.group(2)!!.toLong()
                    val millisecondsString = matcher.group(3)!!
                    var milliseconds = millisecondsString.toLong()
                    if (millisecondsString.length == 2) milliseconds *= 10L
                    val position = (minutes * 60 + seconds) * 1000 + milliseconds
                    val text: String = matcher.group(4)!!.trim()
                    if (text.isNotEmpty()) {
                        if (text.length > maxLengthText.length) maxLengthText = text
                        items += LineItem(position, text)
                        plainText.appendLine(text)
                    }
                }
            }
            // 后二空行
            items += LineItem(Long.MAX_VALUE - 1, "")
            items += LineItem(Long.MAX_VALUE, "")
            // 排序歌词时间顺序
            if (items.size < 11) throw Exception()
            items.sortWith { o1, o2 -> o1.position.compareTo(o2.position) }
            LrcData(plainText.trimEnd().toString(), items, maxLengthText)
        } catch (_: Exception) { null }
    }
}