package com.yinlin.rachel.data.weibo

data class WeiboComment (
    val user: WeiboUser, // 用户
    var time: String, // 时间
    var text: String, // 内容
    var pic: String? = null, // 图片
    var subComments: WeiboCommentList? = null // 楼中楼
)

typealias WeiboCommentList = MutableList<WeiboComment>