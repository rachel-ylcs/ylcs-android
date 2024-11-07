package com.yinlin.rachel.data.topic

import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.user.User.Companion.Level

data class Topic (
    val tid: Int,
    val ts: String,
    val title: String,
    val content: String,
    val pics: List<String>,
    val isTop: Boolean,
    val coinNum: Int,
    val commentNum: Int,
    val uid: Int,
    val name: String,
    val label: String,
    val coin: Int,
    val comments: CommentList,
) {
    val level: Int get() = Level.level(coin)

    val avatarPath: String get() = "${API.BASEURL}/public/users/${uid}/avatar.webp"
    fun picPath(pic: String) = "${API.BASEURL}/public/users/${uid}/pics/${pic}.webp"
}