package com.yinlin.rachel.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.yinlin.rachel.tool.Net
import com.yinlin.rachel.data.Login
import com.yinlin.rachel.data.SendComment
import com.yinlin.rachel.data.SendTopic
import com.yinlin.rachel.data.UpdateToken
import com.yinlin.rachel.data.activity.ShowActivity
import com.yinlin.rachel.data.activity.ShowActivityPreviewList
import com.yinlin.rachel.data.music.PlaylistMap
import com.yinlin.rachel.data.res.ResFile
import com.yinlin.rachel.data.res.ResFolder
import com.yinlin.rachel.data.sys.ServerInfo
import com.yinlin.rachel.data.topic.Topic
import com.yinlin.rachel.data.topic.TopicPreviewList
import com.yinlin.rachel.data.user.MailList
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.data.user.UserProfile
import com.yinlin.rachel.tool.gson
import com.yinlin.rachel.tool.jsonMap
import java.lang.reflect.Type


object API {
    const val BASEURL: String = "https://api.yinlin.love"

    object Code {
        const val SUCCESS = 0
        const val FORBIDDEN = 1
        const val UNAUTHORIZED = 2
        const val FAILED = 3
    }

    class ResultData<T>(json: JsonElement, type: Type? = null) {
        var code: Int = Code.FORBIDDEN
        var msg: String = ""
        private var mData: T? = null
        val data: T get() = mData!!

        init {
            try {
                val jsonObject = json.asJsonObject
                code = jsonObject["code"]?.asInt ?: Code.FORBIDDEN
                msg = jsonObject["msg"]?.asString ?: ""
                if (code == Code.SUCCESS) {
                    val jsonData = jsonObject["data"]
                    if (type != null) {
                        if (jsonData == null) makeError()
                        else {
                            mData = gson.fromJson<T>(jsonData, type)
                            if (mData == null) makeError()
                        }
                    }
                    else if (jsonData != null) makeError()
                }
            }
            catch (_: Exception) { makeError() }
        }

        val success: Boolean get() = code == Code.SUCCESS

        private fun makeError() {
            code = Code.FORBIDDEN
            msg = "网络异常"
        }
    }

    inline fun <reified T> JsonElement.result() = ResultData<T>(this, object : TypeToken<T>(){}.type)
    val JsonElement.msgResult get() = ResultData<Unit>(this)
    inline fun <reified T> errResult() = ResultData<T>(jsonMap("code" to Code.FORBIDDEN, "msg" to "网络异常"))
    
    object CommonAPI {
        private const val BASEURL: String = "${API.BASEURL}/common"

        private fun parseResFolder(parent: ResFolder?, name: String, json: JsonObject): ResFolder? = try {
            val author = if (json.has("author")) json["author"].asString else parent!!.author
            val root = ResFolder(parent, name, author)
            if (json.has("folders")) {
                json.getAsJsonObject("folders").asMap().forEach { (folderName, value) ->
                    parseResFolder(root, folderName, value.asJsonObject)?.let { root.items += it }
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
        catch (_: Exception) { null }

        // 取美图信息
        fun getPhotos(): ResFolder = try {
            val photos = Net.post("$BASEURL/getPhotos").result<JsonObject>().data
            parseResFolder(null, "", photos) ?: ResFolder.emptyRes
        } catch (_: Exception) { ResFolder.emptyRes }

        // 取服务器信息
        fun getServerInfo() = try {
            Net.post("$BASEURL/getServerInfo").result<ServerInfo>()
        } catch (_: Exception) { errResult() }
    }

    object UserAPI {
        private const val BASEURL: String = "${API.BASEURL}/user"

        // 登录
        fun login(name: String, pwd: String) = try {
            Net.post("$BASEURL/login", jsonMap("name" to name, "pwd" to pwd)).result<Login>()
        } catch (_: Exception) { errResult() }

        // 注销
        fun logoff(token: String) = try {
            Net.post("$BASEURL/logoff", jsonMap("token" to token)).msgResult
        } catch (_: Exception) { errResult() }

        // 更新Token
        fun updateToken(token: String) = try {
            Net.post("$BASEURL/updateToken", jsonMap("token" to token)).result<UpdateToken>()
        } catch (_: Exception) { errResult() }

        // 注册
        fun register(name: String, pwd: String, inviterName: String) = try {
            Net.post("$BASEURL/register", jsonMap("name" to name, "pwd" to pwd, "inviterName" to inviterName)).msgResult
        } catch (_: Exception) { errResult() }

        // 忘记密码
        fun forgotPassword(name: String, pwd: String) = try {
            Net.post("$BASEURL/forgotPassword", jsonMap("name" to name, "pwd" to pwd)).msgResult
        } catch (_: Exception) { errResult() }

        // 取用户信息
        fun getInfo(token: String) = try {
            Net.post("$BASEURL/getInfo", jsonMap("token" to token)).result<User>()
        } catch (_: Exception) { errResult() }

        // 更新昵称
        fun updateName(token: String, name: String) = try {
            Net.post("$BASEURL/updateName", jsonMap("token" to token, "name" to name)).msgResult
        } catch (_: Exception) { errResult() }

        // 更新头像
        fun updateAvatar(token: String, filename: String) = try {
            Net.postForm("$BASEURL/updateAvatar", mapOf("avatar" to filename), mapOf("token" to token)).msgResult
        } catch (_: Exception) { errResult() }

        // 更新个性签名
        fun updateSignature(token: String, signature: String) = try {
            Net.post("$BASEURL/updateSignature", jsonMap("token" to token, "signature" to signature)).msgResult
        } catch (_: Exception) { errResult() }

        // 更新背景墙
        fun updateWall(token: String, filename: String) = try {
            Net.postForm("$BASEURL/updateWall", mapOf("wall" to filename), mapOf("token" to token)).msgResult
        } catch (_: Exception) { errResult() }

        // 歌单云备份
        fun uploadPlaylist(token: String, playlist: PlaylistMap) = try {
            Net.post("$BASEURL/uploadPlaylist", jsonMap("token" to token, "playlist" to playlist)).msgResult
        } catch (_: Exception) { errResult() }

        // 歌单云还原
        fun downloadPlaylist(token: String) = try {
            Net.post("$BASEURL/downloadPlaylist", jsonMap("token" to token)).result<PlaylistMap>()
        } catch (_: Exception) { errResult() }

        // 签到
        fun signin(token: String) = try {
            Net.post("$BASEURL/signin", jsonMap("token" to token)).msgResult
        } catch (_: Exception) { errResult() }

        // 提交反馈
        fun sendFeedback(token: String, content: String) = try {
            Net.post("$BASEURL/sendFeedback", jsonMap("token" to token, "content" to content)).msgResult
        } catch (_: Exception) { errResult() }

        // 临时下载4K原图
        fun download4KRes(token: String) = try {
            Net.post("$BASEURL/download4KRes", jsonMap("token" to token)).msgResult
        } catch (_: Exception) { errResult() }

        // 查询邮件
        fun getMail(token: String) = try {
            Net.post("$BASEURL/getMail", jsonMap("token" to token)).result<MailList>()
        } catch (_: Exception) { errResult() }

        // 处理邮件
        fun processMail(token: String, mid: Long, confirm: Boolean) = try {
            Net.post("$BASEURL/processMail", jsonMap("token" to token, "mid" to mid, "confirm" to confirm)).msgResult
        } catch (_: Exception) { errResult() }

        // 删除邮件
        fun deleteMail(token: String, mid: Long) = try {
            Net.post("$BASEURL/deleteMail", jsonMap("token" to token, "mid" to mid)).msgResult
        } catch (_: Exception) { errResult() }

        // 取资料卡
        fun getProfile(uid: Int) = try {
            Net.post("$BASEURL/getProfile", jsonMap("uid" to uid)).result<UserProfile>()
        } catch (_: Exception) { errResult() }

        // 取最新主题
        fun getLatestTopic(upper: Int = 2147483647) = try {
            Net.post("$BASEURL/getLatestTopic", jsonMap("upper" to upper)).result<TopicPreviewList>()
        } catch (_: Exception) { errResult() }

        // 取热门主题
        fun getHotTopic(offset: Int = 0) = try {
            Net.post("$BASEURL/getHotTopic", jsonMap("offset" to offset)).result<TopicPreviewList>()
        } catch (_: Exception) { errResult() }

        // 取主题
        fun getTopic(tid: Int) = try {
            Net.post("$BASEURL/getTopic", jsonMap("tid" to tid)).result<Topic>()
        } catch (_: Exception) { errResult() }

        // 发主题
        fun sendTopic(token: String, title: String, content: String, files: List<String>) = try {
            val fileMap = LinkedHashMap<String, String>()
            for (index in 0 until minOf(files.size, 9)) fileMap["pic${index + 1}"] = files[index]
            Net.postForm("$BASEURL/sendTopic", fileMap, mapOf("token" to token, "title" to title, "content" to content)).result<SendTopic>()
        } catch (_: Exception) { errResult() }

        // 评论
        fun sendComment(token: String, tid: Int, content: String) = try {
            Net.post("$BASEURL/sendComment", jsonMap("token" to token, "tid" to tid, "content" to content)).result<SendComment>()
        } catch (_: Exception) { errResult() }

        // 投币
        fun sendCoin(token: String, desUid: Int, tid: Int, value: Int) = try {
            Net.post("$BASEURL/sendCoin", jsonMap("token" to token, "desUid" to desUid, "tid" to tid, "value" to value)).msgResult
        } catch (_: Exception) { errResult() }

        // 删除评论
        fun deleteComment(token: String, cid: Long, tid: Int) = try {
            Net.post("$BASEURL/deleteComment", jsonMap("token" to token, "cid" to cid, "tid" to tid)).msgResult
        } catch (_: Exception) { errResult() }

        // 更新评论置顶
        fun updateCommentTop(token: String, cid: Long, tid: Int, type: Boolean) = try {
            Net.post("$BASEURL/updateCommentTop", jsonMap("token" to token, "cid" to cid, "tid" to tid, "type" to type)).msgResult
        } catch (_: Exception) { errResult() }

        // 删除主题
        fun deleteTopic(token: String, tid: Int) = try {
            Net.post("$BASEURL/deleteTopic", jsonMap("token" to token, "tid" to tid)).msgResult
        } catch (_: Exception) { errResult() }

        // 更新主题置顶
        fun updateTopicTop(token: String, tid: Int, type: Boolean) = try {
            Net.post("$BASEURL/updateTopicTop", jsonMap("token" to token, "tid" to tid, "type" to type)).msgResult
        } catch (_: Exception) { errResult() }

        // 取活动列表
        fun getActivities() = try {
            Net.post("${BASEURL}/getActivities").result<ShowActivityPreviewList>()
        } catch (_: Exception) { errResult() }

        // 取活动详情
        fun getActivityInfo(ts: String) = try {
            Net.post("$BASEURL/getActivityInfo", jsonMap("ts" to ts)).result<ShowActivity>()
        } catch (_: Exception) { errResult() }

        // 添加活动
        fun addActivity(token: String, show: ShowActivity) = try {
            val fileMap = LinkedHashMap<String, String>()
            for (index in 0 until minOf(show.pics.size, 9)) fileMap["pic${index + 1}"] = show.pics[index]
            val argMap = mutableMapOf("token" to token, "ts" to show.ts, "title" to show.title, "content" to show.content)
            show.showstart?.let { argMap["showstart"] = it }
            show.damai?.let { argMap["damai"] = it }
            show.maoyan?.let { argMap["maoyan"] = it }
            Net.postForm("$BASEURL/addActivity", fileMap, argMap).msgResult
        } catch (_: Exception) { errResult() }

        // 删除活动
        fun deleteActivity(token: String, ts: String) = try {
            Net.post("$BASEURL/deleteActivity", jsonMap("token" to token, "ts" to ts)).msgResult
        } catch (_: Exception) { errResult() }
    }
}