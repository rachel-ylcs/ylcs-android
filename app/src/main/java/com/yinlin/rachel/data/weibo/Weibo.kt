package com.yinlin.rachel.data.weibo

import com.yinlin.rachel.model.RachelPreview

data class Weibo(
    var id: String, // 编号
    val user: WeiboUser, // 用户
    var time: String, // 时间
    var text: String, // 内容
    val commentNum: Int, // 评论数
    val likeNum: Int, // 点赞数
    val pictures: List<RachelPreview>, // 图片集
)

typealias WeiboList = MutableList<Weibo>