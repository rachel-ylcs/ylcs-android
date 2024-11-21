package com.yinlin.rachel.data.weibo

import com.yinlin.rachel.model.RachelPreview

data class WeiboComment (
    val user: WeiboCommentUser, // 用户
    var time: String, // 时间
    var text: String, // 内容
    var pic: RachelPreview? = null, // 图片
    var subComments: WeiboCommentList? = null // 楼中楼
)

typealias WeiboCommentList = MutableList<WeiboComment>