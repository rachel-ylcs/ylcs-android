package com.yinlin.rachel.data.weibo

open class WeiboUserStorage(
    val userId: String,
    var name: String,
    var avatar: String
) {
    companion object {
        val defaultWeiboUsers get() = mutableListOf(
            WeiboUserStorage("2266537042", "", ""),
            WeiboUserStorage("7802114712", "", ""),
            WeiboUserStorage("3965226022", "", "")
        )
    }
}

typealias WeiboUserStorageList = MutableList<WeiboUserStorage>