package com.yinlin.rachel.data.weibo

data class WeiboUserInfo (
    var userId: String, // 用户ID
    var name: String, // 昵称
    var avatar: String, // 头像
    var signature: String, // 个性签名
    var followNum: String, // 关注数
    var fansNum: String, // 粉丝数
)