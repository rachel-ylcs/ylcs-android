package com.yinlin.rachel.data.weibo

import com.yinlin.rachel.jsonString

@JvmRecord
data class WeiboUserStorage(
    val name: String,
    val containerId: String
) {
    companion object {
        val defaultWeiboUsers: String get() = linkedMapOf(
            "2266537042" to WeiboUserStorage("银临Rachel", "1076032266537042"),
            "7802114712" to WeiboUserStorage("银临-欢银光临", "1076037802114712"),
            "3965226022" to WeiboUserStorage("银临的小银库", "1076033965226022")
        ).jsonString
    }
}

typealias WeiboUserStorageMap = MutableMap<String, WeiboUserStorage>

val WeiboUserStorageMap.names: List<String> get() = this.map { it.value.name }