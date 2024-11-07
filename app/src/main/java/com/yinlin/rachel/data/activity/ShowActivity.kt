package com.yinlin.rachel.data.activity

import com.yinlin.rachel.api.API

class ShowActivity(
    ts: String,
    title: String,
    val content: String,
    val pics: List<String>,
    val showstart: String?,
    val damai: String?,
    val maoyan: String?
) : ShowActivityPreview(ts, title) {
    fun picPath(pic: String) = "${API.BASEURL}/public/activity/${pic}.webp"
}