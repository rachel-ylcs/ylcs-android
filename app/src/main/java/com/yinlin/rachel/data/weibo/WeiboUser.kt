package com.yinlin.rachel.data.weibo

class WeiboUser(
    userId: String, // 用户ID
    name: String, // 昵称
    avatar: String, // 头像
    val background: String, // 背景图
    var signature: String, // 个性签名
    var followNum: String, // 关注数
    var fansNum: String, // 粉丝数
) : WeiboUserStorage(userId, name, avatar)