package com.yinlin.rachel.data.weibo

class WeiboCommentUser(
    userId: String, // 用户ID
    name: String, // 昵称
    avatar: String, // 头像
    var location: String, // 定位
) : WeiboUserStorage(userId, name, avatar)