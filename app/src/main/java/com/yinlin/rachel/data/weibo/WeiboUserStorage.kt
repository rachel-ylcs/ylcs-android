package com.yinlin.rachel.data.weibo

import com.yinlin.rachel.jsonString

open class WeiboUserStorage(
    val userId: String,
    var name: String,
    var avatar: String
) {
    companion object {
        val defaultWeiboUsers: String = listOf(
            WeiboUserStorage("2266537042", "", ""),
            WeiboUserStorage("7802114712", "", ""),
            WeiboUserStorage("3965226022", "", "")
        ).jsonString
    }
}

typealias WeiboUserStorageList = MutableList<WeiboUserStorage>