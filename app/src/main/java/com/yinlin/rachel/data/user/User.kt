package com.yinlin.rachel.data.user

import com.yinlin.rachel.api.API

data class User(
    // ID
    val uid: Int,
    // 昵称
    val name: String,
    // 邀请人
    val inviterName: String?,
    // 权限
    val privilege: Int,
    // 个性签名
    val signature: String,
    // 头衔
    val label: String,
    // 银币
    val coin: Int,
) {
    companion object {
        const val RENAME_COIN_COST = 5

        object Constraint {
            const val MIN_USER_NAME_LENGTH = 2
            const val MAX_USER_NAME_LENGTH = 16
            const val MIN_PWD_LENGTH = 6
            const val MAX_PWD_LENGTH = 18

            fun name(vararg args: String) = args.all { it.length in MIN_USER_NAME_LENGTH..MAX_USER_NAME_LENGTH }
            fun password(vararg args: String) = args.all { it.length in MIN_PWD_LENGTH..MAX_PWD_LENGTH }
        }

        object Level {
            // 等级对照表
            private val Default = intArrayOf(
                0, 5, 10, 20, 30,
                50, 75, 100, 125, 150,
                200, 250, 300, 350, 400,
                500, 600, 700, 800, 900,
            )

            fun level(num: Int): Int {
                if (num > 0) {
                    for (i in Default.indices) {
                        if (num >= Default[i] && (i == Default.size - 1 || num < Default[i + 1])) return i + 1
                    }
                }
                return 1
            }
        }

        object Privilege {
            private const val BACKUP = 1 // 备份
            private const val RES = 2 // 美图
            private const val TOPIC = 4 // 主题
            private const val UNUSED = 8 // 未定
            private const val VIP_ACCOUNT = 16 // 账号管理
            private const val VIP_TOPIC = 32 // 主题管理
            private const val VIP_CALENDAR = 64 // 日历管理

            fun has(privilege: Int, mask: Int): Boolean = (privilege and mask) != 0
            fun backup(privilege: Int): Boolean = has(privilege, BACKUP)
            fun res(privilege: Int): Boolean = has(privilege, RES)
            fun topic(privilege: Int): Boolean = has(privilege, TOPIC)
            fun vipAccount(privilege: Int): Boolean = has(privilege, VIP_ACCOUNT)
            fun vipTopic(privilege: Int): Boolean = has(privilege, VIP_TOPIC)
            fun vipCalendar(privilege: Int): Boolean = has(privilege, VIP_CALENDAR)
        }
    }

    val level: Int get() = Level.level(coin)

    val avatarPath: String get() = "${API.BASEURL}/public/users/${uid}/avatar.webp"
    val wallPath: String get() = "${API.BASEURL}/public/users/${uid}/wall.webp"

    val hasPrivilegeBackup: Boolean get() = Privilege.backup(privilege)
    val hasPrivilegeRes: Boolean get() = Privilege.res(privilege)
    val hasPrivilegeTopic: Boolean get() = Privilege.topic(privilege)
    val hasPrivilegeVIPAccount: Boolean get() = Privilege.vipAccount(privilege)
    val hasPrivilegeVIPTopic: Boolean get() = Privilege.vipTopic(privilege)
    val hasPrivilegeVIPCalendar: Boolean get() = Privilege.vipCalendar(privilege)

    fun canUpdateTopicTop(topicUid: Int) = uid == topicUid
    fun canDeleteTopic(topicUid: Int) = uid == topicUid || hasPrivilegeVIPTopic
    fun canUpdateCommentTop(topicUid: Int) = uid == topicUid || hasPrivilegeVIPTopic
    fun canDeleteComment(topicUid: Int, commentUid: Int) = uid == topicUid || uid == commentUid || hasPrivilegeVIPTopic
    val canDownload4KRes: Boolean get() = hasPrivilegeRes && level >= 8
}