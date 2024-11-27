package com.yinlin.rachel.data.neteasecloud

data class CloudMusic(
    val id: String, // 音乐 ID
    val name: String, // 名称
    val singer: String, // 歌手
    val time: String, // 时长
    val pic: String, // 封面
    val lyrics: String, // 歌词
    val mp3Url: String, // MP3 下载链接
)