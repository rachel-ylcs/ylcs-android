package com.yinlin.rachel.data.music

data class PlaylistPreview (val name: String, val items: List<MusicItem>) {
    data class MusicItem(
        val id: String,
        val name: String,
        val singer: String,
        val isDeleted: Boolean = false
    )
}