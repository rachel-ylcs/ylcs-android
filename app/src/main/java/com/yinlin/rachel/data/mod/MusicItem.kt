package com.yinlin.rachel.data.mod;

data class MusicItem(
    val name: String,
    val version: String,
    val resList: MutableList<String> = ArrayList()
)