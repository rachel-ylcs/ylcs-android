package com.yinlin.rachel.data

data class Login(val token: String)
data class UpdateToken(val token: String)
data class SendTopic(val tid: Int, val pic: String?)
data class SendComment(val cid: Long, val ts: String)