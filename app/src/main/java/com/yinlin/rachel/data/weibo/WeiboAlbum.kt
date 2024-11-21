package com.yinlin.rachel.data.weibo

data class WeiboAlbum(
    val containerId: String,
    val title: String,
    val num: String,
    val time: String,
    val pic: String
)

typealias WeiboAlbumList = MutableList<WeiboAlbum>