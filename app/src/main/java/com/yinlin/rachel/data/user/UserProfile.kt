package com.yinlin.rachel.data.user

import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.topic.TopicPreviewList

@JvmRecord
data class UserProfile(
    // ID
    val uid: Int,
    // 昵称
    val name: String,
    // 个性签名
    val signature: String,
    // 头衔
    val label: String,
    // 银币
    val coin: Int,
    // 主题
    val topics: TopicPreviewList,
) {
    val avatarPath: String get() = "${API.BASEURL}/public/users/${uid}/avatar.webp"
    val wallPath: String get() = "${API.BASEURL}/public/users/${uid}/wall.webp"

    val level: Int get() = User.Companion.Level.level(coin)
}