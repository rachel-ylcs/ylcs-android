package com.yinlin.rachel.data.user


data class Mail(
    val mid: Long,
    val uid: Int,
    val ts: String,
    val type: Int,
    var processed: Boolean,
    val title: String,
    val content: String
) {
    companion object {
        const val TYPE_INFO = 1
        const val TYPE_CONFIRM = 2
        const val TYPE_DECISION = 4
        const val TYPE_INPUT = 8
    }
}

typealias MailList = List<Mail>