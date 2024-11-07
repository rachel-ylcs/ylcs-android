package com.yinlin.rachel.data.topic

import com.yinlin.rachel.api.API

data class TopicPreview(
    val tid: Int,
    val uid: Int,
    val name: String,
    val title: String,
    var pic: String?,
    var isTop: Boolean,
    val coinNum: Int,
    val commentNum: Int,
) {
    val picPath: String get() = "${API.BASEURL}/public/users/${uid}/pics/${pic}.webp"
    val avatarPath: String get() = "${API.BASEURL}/public/users/${uid}/avatar.webp"
}

typealias TopicPreviewList = List<TopicPreview>