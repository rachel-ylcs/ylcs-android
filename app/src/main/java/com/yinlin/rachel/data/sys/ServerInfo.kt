package com.yinlin.rachel.data.sys

data class ServerInfo(
    val targetVersion: Long,
    val minVersion: Long,
    val downloadUrl: String?,
    val developState: DevelopStateList,
)