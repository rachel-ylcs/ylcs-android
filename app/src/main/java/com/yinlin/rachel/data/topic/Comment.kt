package com.yinlin.rachel.data.topic

import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.user.User.Companion.Level

data class Comment (
    val cid: Long,
    val uid: Int,
    val name: String,
    val ts: String,
    val content: String,
    var isTop: Boolean,
    val label: String,
    val coin: Int
) {
    val level: Int get() = Level.level(coin)

    val avatarPath: String get() = "${API.BASEURL}/public/users/${uid}/avatar.webp"
}

typealias CommentList = List<Comment>