package com.yinlin.rachel.data.music

import com.yinlin.rachel.div
import com.yinlin.rachel.model.RachelMod
import com.yinlin.rachel.pathMusic

data class MusicInfoPreview(
    val version: String,
    val id: String,
    val name: String,
    val singer: String,
    var selected: Boolean = false,
) {
    val recordPath get() = pathMusic / (id + RachelMod.RES_RECORD)
}

typealias MusicInfoPreviewList = List<MusicInfoPreview>