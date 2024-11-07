package com.yinlin.rachel.data.mod

import com.yinlin.rachel.model.RachelMod.MOD_VERSION

class Metadata {
    var version: Int = MOD_VERSION // MOD版本
    var config: String = "" // MOD配置
    val items = HashMap<String, MusicItem>() // 音乐集

    val empty: Boolean get() = items.isEmpty()
    val totalCount: Int get() = items.entries.sumOf { it.value.resList.size }
}