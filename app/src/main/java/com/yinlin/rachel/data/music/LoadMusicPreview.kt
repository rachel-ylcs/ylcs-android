package com.yinlin.rachel.data.music

data class LoadMusicPreview(
    val id: String,
    val name: String,
    val singer: String,
    val isDeleted: Boolean = false,
    val isPlaying: Boolean = false,
)

typealias LoadMusicPreviewList = List<LoadMusicPreview>