package com.yinlin.rachel.data.music

data class PlayingMusicPreview(
    val id: String,
    val name: String,
    val singer: String,
    val isPlaying: Boolean = false
)

typealias PlayingMusicPreviewList = List<PlayingMusicPreview>