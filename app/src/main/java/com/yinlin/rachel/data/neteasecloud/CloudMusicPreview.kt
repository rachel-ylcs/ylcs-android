package com.yinlin.rachel.data.neteasecloud

data class CloudMusicPreview(
    val id: String, // 音乐 ID
    val name: String, // 名称
    val singer: String, // 歌手
    val time: String, // 时长
)

typealias CloudMusicPreviewList = List<CloudMusicPreview>