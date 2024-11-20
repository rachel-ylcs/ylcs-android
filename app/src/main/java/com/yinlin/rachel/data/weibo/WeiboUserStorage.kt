package com.yinlin.rachel.data.weibo

import com.yinlin.rachel.jsonString

data class WeiboUserStorage(
    val userId: String,
    var containerId: String,
    var name: String,
    var avatar: String
) {
    companion object {
        val defaultWeiboUsers: String = listOf(
            WeiboUserStorage("2266537042", "1076032266537042", "银临Rachel", ""),
            WeiboUserStorage("7802114712", "1076037802114712", "银临-欢银光临", ""),
            WeiboUserStorage("3965226022", "1076033965226022", "银临的小银库", "")
        ).jsonString
    }
}

typealias WeiboUserStorageList = MutableList<WeiboUserStorage>