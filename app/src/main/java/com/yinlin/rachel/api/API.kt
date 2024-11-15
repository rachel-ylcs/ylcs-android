package com.yinlin.rachel.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.yinlin.rachel.Net
import com.yinlin.rachel.data.res.ResFile
import com.yinlin.rachel.data.res.ResFolder
import com.yinlin.rachel.data.sys.ServerInfo
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.jsonMap
import com.yinlin.rachel.fetch


object API {
    const val BASEURL: String = "https://api.yinlin.love"

    object Code {
        const val SUCCESS = 0
        const val FORBIDDEN = 1
        const val UNAUTHORIZED = 2
        const val FAILED = 3
    }

    class Result(json: JsonElement) {
        companion object {
            val ERROR = Result(jsonMap("code" to Code.FORBIDDEN, "msg" to "网络异常"))
        }

        var data: JsonObject? = null

        var code: Int = Code.FORBIDDEN
        var success: Boolean = false
        var msg: String = ""

        init {
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                code = obj["code"]?.asInt ?: Code.FORBIDDEN
                success = code == Code.SUCCESS
                msg = obj["msg"]?.asString ?: ""
                data = obj["data"]?.let { if (it.isJsonObject) it.asJsonObject else null }
            }
        }

        operator fun get(key: String): JsonElement = data!![key]

        inline fun <reified T> fetch(): T = data!!.fetch()
    }

    object CommonAPI {
        private const val BASEURL: String = "${API.BASEURL}/common"

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
        catch (_: Exception) { null }

        // 取美图信息
        fun getPhotos(): ResFolder = try {
            val photos = Result(Net.post("$BASEURL/getPhotos")).data!!
            parseResFolder(null, "", photos) ?: ResFolder.emptyRes
        } catch (_: Exception) { ResFolder.emptyRes }

        // 取服务器信息
        fun getServerInfo(): Result = try {
            Result(Net.post("$BASEURL/getServerInfo"))
        } catch (_: Exception) { Result.ERROR }
    }

    object UserAPI {
        private const val BASEURL: String = "${API.BASEURL}/user"

        // 登录
        fun login(name: String, pwd: String) = try {
            Result(Net.post("$BASEURL/login", jsonMap("name" to name, "pwd" to pwd)))
        } catch (_: Exception) { Result.ERROR }

        // 注销
        fun logoff(token: String) = try {
            Result(Net.post("$BASEURL/logoff", jsonMap("token" to token)))
        } catch (_: Exception) { Result.ERROR }

        // 更新Token
        fun updateToken(token: String) = try {
            Result(Net.post("$BASEURL/updateToken", jsonMap("token" to token)))
        } catch (_: Exception) { Result.ERROR }

        // 注册
        fun register(name: String, pwd: String, inviterName: String) = try {
            Result(Net.post("$BASEURL/register", jsonMap("name" to name, "pwd" to pwd, "inviterName" to inviterName)))
        } catch (_: Exception) { Result.ERROR }

        // 忘记密码
        fun forgotPassword(name: String, pwd: String) = try {
            Result(Net.post("$BASEURL/forgotPassword", jsonMap("name" to name, "pwd" to pwd)))
        } catch (_: Exception) { Result.ERROR }

        // 取用户信息
        fun getInfo(token: String): Result = try {
            Result(Net.post("$BASEURL/getInfo", jsonMap("token" to token)))
        } catch (_: Exception) { Result.ERROR }

        // 更新昵称
        fun updateName(token: String, name: String) = try {
            Result(Net.post("$BASEURL/updateName", jsonMap("token" to token, "name" to name)))
        } catch (_: Exception) { Result.ERROR }

        // 更新头像
        fun updateAvatar(token: String, filename: String) = try {
            Result(Net.postForm("$BASEURL/updateAvatar", mapOf("avatar" to filename), mapOf("token" to token)))
        } catch (ignored: Exception) { Result.ERROR }
    }
}