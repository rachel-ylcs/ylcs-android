package com.yinlin.rachel.data.music

import com.yinlin.rachel.tool.div
import com.yinlin.rachel.tool.pathMusic

data class MusicInfoPreview(
    val version: String,
    val id: String,
    val name: String,
    val singer: String,
    var selected: Boolean = false,
) {
    val recordPath get() = pathMusic / (id + MusicRes.RECORD_NAME)

    companion object {
        val MusicInfo.preview get() = MusicInfoPreview(version, id, name, singer, false)
    }
}

typealias MusicInfoPreviewList = List<MusicInfoPreview>