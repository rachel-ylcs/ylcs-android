package com.yinlin.rachel.data.music


data class Playlist(
    var name: String,
    val items: MutableList<String>
) {
    constructor(name: String, item: String): this(name, ArrayList<String>()) {
        items += item
    }
}

typealias PlaylistMap = MutableMap<String, Playlist>