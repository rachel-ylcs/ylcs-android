package com.yinlin.rachel.data.music

import com.yinlin.rachel.div
import com.yinlin.rachel.model.engine.LineLyricsEngine
import com.yinlin.rachel.parseJsonObject
import com.yinlin.rachel.pathMusic
import com.yinlin.rachel.writeJson

data class MusicInfo (
    val version: String, // 版本号
    val author: String, // 作者
    val id: String, // 编号
    var name: String, // 歌名
    var singer: String, // 歌手
    var lyricist: String, // 词作
    var composer: String, // 曲作
    var album: String, // 专辑
    var bgd: Boolean, // 是否有动态背景
    var video: Boolean, // 是否有MV
    val chorus: ChorusList = mutableListOf(), // 副歌点
    var lyrics: LyricsFileMap = mutableMapOf(), // 歌词引擎
    // 延迟加载属性
    var lrcData: LrcData? = null, // LRC歌词
) {
    val isCorrect: Boolean get() {
        if (id.isEmpty()) return false
        if (!audioPath.exists()) return false
        if (!recordPath.exists()) return false
        if (!bgsPath.exists()) return false
        if (bgd && !bgdPath.exists()) return false
        if (video && !videoPath.exists()) return false
        if (lyrics.isEmpty()) return false
        val lineEngine = lyrics[LineLyricsEngine.NAME]
        return !(lineEngine.isNullOrEmpty() || !lineEngine.contains(""))
    }

    val infoPath get() = pathMusic / (id + MusicRes.INFO_NAME)
    val audioPath get() = pathMusic / (id + MusicRes.AUDIO_NAME)
    val recordPath get() = pathMusic / (id + MusicRes.RECORD_NAME)
    val defaultLrcPath get() = pathMusic / (id + MusicRes.DEFAULT_LRC_NAME)
    val videoPath get() = pathMusic / (id + MusicRes.VIDEO_NAME)
    val bgsPath get() = pathMusic / (id + MusicRes.BGS_NAME)
    val bgdPath get() = pathMusic / (id + MusicRes.BGD_NAME)

    fun customPath(name: String) = pathMusic / (id + name)

    fun rewrite() {
        val json = this.parseJsonObject
        json.remove("lrcData")
        infoPath.writeJson(json)
    }
}

typealias ChorusList = MutableList<Long>
typealias LyricsFileMap = MutableMap<String, MutableList<String>>