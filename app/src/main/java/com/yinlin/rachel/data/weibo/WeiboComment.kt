package com.yinlin.rachel.data.weibo

data class WeiboComment (
    var type: Type, // 评论类型
    val user: WeiboUser, // 用户
    var time: String, // 时间
    var text: String, // 内容
    var pic: String = "", // 图片
) {
    enum class Type { Comment, SubComment }
}

typealias WeiboCommentList = MutableList<WeiboComment>