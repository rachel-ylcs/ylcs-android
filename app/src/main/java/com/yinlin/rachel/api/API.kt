package com.yinlin.rachel.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.yinlin.rachel.Net
import com.yinlin.rachel.asStringOrNull
import com.yinlin.rachel.data.topic.Comment
import com.yinlin.rachel.data.res.ResFile
import com.yinlin.rachel.data.res.ResFolder
import com.yinlin.rachel.data.sys.ServerInfo
import com.yinlin.rachel.data.activity.ShowActivity
import com.yinlin.rachel.data.activity.ShowActivityPreviewList
import com.yinlin.rachel.data.music.PlaylistMap
import com.yinlin.rachel.data.topic.Topic
import com.yinlin.rachel.data.topic.TopicPreviewList
import com.yinlin.rachel.data.user.MailList
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.data.user.UserProfile
import com.yinlin.rachel.jsonMap
import com.yinlin.rachel.fetch
import kotlin.random.Random


object API {
    private fun urlQ(v: Int): String {
        var hc = v + v.hashCode()
        val hr = Random.nextInt(100)
        hc += hr
        hc -= v.hashCode()
        return (hc - hr).toString()
    }

    val BASEURL: String = "http://${urlQ(124)}.${urlQ(221)}.${urlQ(142)}.${urlQ(68)}:${urlQ(12110)}"

    data class Result<T>(val ok: Boolean, val value: T)
    data class Result2<T, U>(val ok: Boolean, val value1: T, val value2: U)
    data class Result3<T, U1, U2>(val ok: Boolean, val value1: T, val value2: U1, val value3: U2)

    private fun JsonElement.fetchMsg(): Result<String> {
        val obj = this.asJsonObject
        return Result(obj["ok"].asBoolean, obj["msg"].asString)
    }
    private inline fun <U> JsonElement.fetchMsg(u: (JsonObject) -> U): Result2<String, U> {
        val obj = this.asJsonObject
        return Result2(obj["ok"].asBoolean, obj["msg"].asString, u(obj))
    }
    private inline fun <U1, U2> JsonElement.fetchMsg(u1: (JsonObject) -> U1, u2: (JsonObject) -> U2): Result3<String, U1, U2> {
        val obj = this.asJsonObject
        return Result3(obj["ok"].asBoolean, obj["msg"].asString, u1(obj), u2(obj))
    }
    private val errResult = Result(false, "网络异常")
    private fun <U> errResult2(u: U) = Result2(false, "网络异常", u)
    private fun <U1, U2> errResult3(u1: U1, u2: U2) = Result3(false, "网络异常", u1, u2)

    object ResAPI {
        private val BASEURL: String = "${API.BASEURL}/res"

        private fun parseResFolder(parent: ResFolder?, name: String, json: JsonObject): ResFolder? = try {
            val author = if (json.has("author")) json["author"].asString else parent!!.author
            val root = ResFolder(parent, name, author)
            if (json.has("folders")) {
                json.getAsJsonObject("folders").asMap().forEach { (folderName, value) ->
                    parseResFolder(root, folderName, value.asJsonObject)?.apply { root.items += this }
                }
            }
            if (json.has("files")) {
                var index = 0
                for (entry in json.getAsJsonArray("files")) {
                    if (entry.isJsonObject) {
                        val file = entry.asJsonObject
                        val fileName = if (file.has("name")) file["name"].asString else index++.toString()
                        val fileAuthor = if (file.has("author")) file["author"].asString else root.author
                        val fileUrl = file["url"].asString
                        root.items += ResFile(root, fileName, fileAuthor, fileUrl)
                    } else {
                        val fileName = index++.toString()
                        val fileUrl = entry.asString
                        root.items += ResFile(root, fileName, root.author, fileUrl)
                    }
                }
            }
            root
        }
        catch (ignored: Exception) { null }

        // 取美图信息
        fun getInfo(): ResFolder = try {
            parseResFolder(null, "", Net.post("$BASEURL/getInfo").asJsonObject) ?: ResFolder.emptyRes
        } catch (ignored: Exception) { ResFolder.emptyRes }
    }

    object SysAPI {
        private val BASEURL: String = "${API.BASEURL}/sys"

        // 取服务器信息
        fun getServerInfo(): ServerInfo? = try {
            Net.post("$BASEURL/getServerInfo").fetch()
        } catch (ignored: Exception) { null }
    }

    object UserAPI {
        private val BASEURL: String = "${API.BASEURL}/user"

        // 提交反馈
        fun sendFeedback(user: User, content: String) = try {
            Net.post("$BASEURL/sendFeedback",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "content" to content)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 临时下载4K原图
        fun download4KRes(user: User) = try {
            Net.post("$BASEURL/download4KRes",
                jsonMap("uid" to user.uid, "pwd" to user.pwd)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 登录
        fun login(name: String, pwd: String): Result2<String, User?> = try {
            Net.post("$BASEURL/login",
                jsonMap("name" to name, "pwd" to pwd)
            ).fetchMsg { it.fetch<User?>()?.apply { this.pwd = pwd } }
        } catch (ignored: Exception) { errResult2(null) }

        // 注册
        fun register(name: String, pwd: String, inviterName: String) = try {
            Net.post("$BASEURL/register",
                jsonMap("name" to name, "pwd" to pwd, "inviterName" to inviterName)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 忘记密码
        fun forgotPassword(name: String, pwd: String) = try {
            Net.post("$BASEURL/forgotPassword",
                jsonMap("name" to name, "pwd" to pwd)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 歌单云备份
        fun uploadPlaylist(user: User, playlist: PlaylistMap) = try {
            Net.post("$BASEURL/uploadPlaylist",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "playlist" to playlist)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 歌单云还原
        fun downloadPlaylist(user: User): PlaylistMap? = try {
            Net.post("$BASEURL/downloadPlaylist",
                jsonMap("uid" to user.uid, "pwd" to user.pwd)
            ).fetch()
        } catch (ignored: Exception) { null }

        // 取用户信息
        fun getInfo(user: User): User? = try {
            Net.post("$BASEURL/getInfo",
                jsonMap("uid" to user.uid, "pwd" to user.pwd)
            ).fetch<User?>()?.apply { this.pwd = user.pwd }
        } catch (ignored: Exception) { null }

        // 更新昵称
        fun updateName(user: User, name: String) = try {
            Net.post("$BASEURL/updateName",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "name" to name)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 更新头像
        fun updateAvatar(user: User, filename: String) = try {
            Net.postForm("$BASEURL/updateAvatar",
                mapOf("avatar" to filename), mapOf("uid" to user.uid.toString(), "pwd" to user.pwd)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 更新个性签名
        fun updateSignature(user: User, signature: String) = try {
            Net.post("$BASEURL/updateSignature",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "signature" to signature)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 更新背景墙
        fun updateWall(user: User, filename: String) = try {
            Net.postForm("$BASEURL/updateWall",
                mapOf("wall" to filename), mapOf("uid" to user.uid.toString(), "pwd" to user.pwd)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 签到
        fun signin(user: User) = try {
            Net.post("$BASEURL/signin",
                jsonMap("uid" to user.uid, "pwd" to user.pwd)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 查询邮件
        fun getMail(user: User): MailList? = try {
            Net.post("$BASEURL/getMail",
                jsonMap("uid" to user.uid, "pwd" to user.pwd)
            ).fetch()
        } catch (ignored: Exception) { null }

        // 处理邮件
        fun processMail(user: User, mid: Long, confirm: Boolean) = try {
            Net.post("$BASEURL/processMail",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "mid" to mid, "confirm" to confirm)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 删除邮件
        fun deleteMail(user: User, mid: Long) = try {
            Net.post("$BASEURL/deleteMail",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "mid" to mid)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 取资料卡
        fun getProfile(uid: Int): UserProfile? = try {
            Net.post("$BASEURL/getProfile",
                jsonMap("uid" to uid)
            ).fetch()
        } catch (ignored: Exception) { null }

        // 取最新主题
        fun getLatestTopic(upper: Int = 2147483647): TopicPreviewList? = try {
            Net.post("$BASEURL/getLatestTopic",
                jsonMap("upper" to upper)
            ).fetch()
        } catch (ignored: Exception) { null }

        // 取热门主题
        fun getHotTopic(offset: Int = 0): TopicPreviewList? = try {
            Net.post("$BASEURL/getHotTopic",
                jsonMap("offset" to offset)
            ).fetch()
        } catch (ignored: Exception) { null }

        // 取主题
        fun getTopic(tid: Int) : Topic? = try {
            Net.post("$BASEURL/getTopic",
                jsonMap("tid" to tid)
            ).fetch()
        } catch (ignored: Exception) { null }

        // 发主题
        fun sendTopic(user: User, title: String, content: String, files: List<String>): Result3<String, Int, String?> = try {
            val fileMap = LinkedHashMap<String, String>()
            for (index in 0 until minOf(files.size, 9)) fileMap["pic${index + 1}"] = files[index]
            Net.postForm("$BASEURL/sendTopic", fileMap,
                mapOf("uid" to user.uid.toString(), "pwd" to user.pwd, "title" to title, "content" to content)
            ).fetchMsg({ it["tid"].asInt }, { it["pic"].asStringOrNull })
        } catch (ignored: Exception) { errResult3(0, null) }

        // 评论
        fun sendComment(user: User, tid: Int, content: String): Result2<String, Comment?> = try {
            Net.post("$BASEURL/sendComment",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "tid" to tid, "content" to content)
            ).fetchMsg { Comment(it["cid"].asLong, user.uid, user.name, it["ts"].asString, content, false, user.label, user.coin) }
        } catch (ignored: Exception) { errResult2(null) }

        // 投币
        fun sendCoin(user: User, desUid: Int, tid: Int, value: Int) = try {
            Net.post("$BASEURL/sendCoin",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "desUid" to desUid, "tid" to tid, "value" to value)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 删除评论
        fun deleteComment(user: User, cid: Long, tid: Int) = try {
            Net.post("$BASEURL/deleteComment",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "cid" to cid, "tid" to tid)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 更新评论置顶
        fun updateCommentTop(user: User, cid: Long, tid: Int, type: Boolean) = try {
            Net.post("$BASEURL/updateCommentTop",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "cid" to cid, "tid" to tid, "type" to type)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 删除主题
        fun deleteTopic(user: User, tid: Int) = try {
            Net.post("$BASEURL/deleteTopic",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "tid" to tid)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 更新主题置顶
        fun updateTopicTop(user: User, tid: Int, type: Boolean) = try {
            Net.post("$BASEURL/updateTopicTop",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "tid" to tid, "type" to type)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 取活动列表
        fun getActivities(): ShowActivityPreviewList? = try {
            Net.post("${BASEURL}/getActivities").fetch()
        } catch (ignored: Exception) { null }

        // 取活动详情
        fun getActivityInfo(ts: String): ShowActivity? = try {
            Net.post("$BASEURL/getActivityInfo",
                jsonMap("ts" to ts)
            ).fetch()
        } catch (ignored: Exception) { null }

        // 添加活动
        fun addActivity(user: User, show: ShowActivity) = try {
            val fileMap = LinkedHashMap<String, String>()
            for (index in 0 until minOf(show.pics.size, 9)) fileMap["pic${index + 1}"] = show.pics[index]
            val argMap = mutableMapOf("uid" to user.uid.toString(), "pwd" to user.pwd, "ts" to show.ts, "title" to show.title, "content" to show.content)
            show.showstart?.apply { argMap["showstart"] = this }
            show.damai?.apply { argMap["damai"] = this }
            show.maoyan?.apply { argMap["maoyan"] = this }
            Net.postForm("$BASEURL/addActivity", fileMap, argMap).fetchMsg()
        } catch (ignored: Exception) { errResult }

        // 删除活动
        fun deleteActivity(user: User, ts: String) = try {
            Net.post("$BASEURL/deleteActivity",
                jsonMap("uid" to user.uid, "pwd" to user.pwd, "ts" to ts)
            ).fetchMsg()
        } catch (ignored: Exception) { errResult }
    }
}