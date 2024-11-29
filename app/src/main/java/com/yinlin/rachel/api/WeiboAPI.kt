package com.yinlin.rachel.api

import android.net.Uri
import com.google.gson.JsonObject
import com.yinlin.rachel.tool.Net
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.data.weibo.WeiboAlbum
import com.yinlin.rachel.data.weibo.WeiboAlbumList
import com.yinlin.rachel.data.weibo.WeiboComment
import com.yinlin.rachel.data.weibo.WeiboCommentList
import com.yinlin.rachel.data.weibo.WeiboContainer
import com.yinlin.rachel.data.weibo.WeiboList
import com.yinlin.rachel.data.weibo.WeiboCommentUser
import com.yinlin.rachel.data.weibo.WeiboUser
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.data.weibo.WeiboUserStorageList
import com.yinlin.rachel.model.RachelPreview
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.jvm.internal.Ref.IntRef


object WeiboAPI {
    fun searchUser(name: String): List<WeiboUserStorage>? = try {
        val result = mutableListOf<WeiboUserStorage>()
        val url = "https://m.weibo.cn/api/container/getIndex?containerid=${WeiboContainer.searchUser(name)}&page_type=searchall"
        val json = Net.get(url).asJsonObject
        val data = json.getAsJsonObject("data")
        for (item1 in data.getAsJsonArray("cards")) {
            val group = item1.asJsonObject
            if (group["card_type"].asInt == 11) {
                for (item2 in group.getAsJsonArray("card_group")) {
                    val card = item2.asJsonObject
                    if (card["card_type"].asInt == 10) {
                        val user = card["user"].asJsonObject
                        val id = user["id"].asString
                        val userName = user["screen_name"].asString
                        val avatar = user["avatar_hd"].asString
                        result += WeiboUserStorage(id, userName, avatar)
                    }
                }
            }
        }
        result
    } catch (_: Exception) { null }

    fun extractWeiboUserStorage(uid: String): WeiboUserStorage? = try {
        val url = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid"
        val json = Net.get(url).asJsonObject
        val userInfo = json.getAsJsonObject("data").getAsJsonObject("userInfo")
        val userId = userInfo["id"].asString
        val name = userInfo["screen_name"].asString
        val avatar = userInfo["avatar_hd"].asString
        WeiboUserStorage(userId, name, avatar)
    }
    catch (_: Exception) { null }

    private fun extractWeibo(card: JsonObject): Weibo {
        var blogs = card.getAsJsonObject("mblog")
        // 提取ID
        val blogId = blogs["id"].asString
        // 提取名称和头像
        val user = blogs.getAsJsonObject("user")
        val userId = user["id"].asString
        val userName = user["screen_name"].asString
        val avatar = user["avatar_hd"].asString
        // 提取时间
        val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        var formattedTime = "未知时间"
        try {
            val date = inputFormat.parse(blogs["created_at"].asString)
            formattedTime = outputFormat.format(date!!)
        } catch (_: Exception) { }
        // 提取IP
        var location = "IP未知"
        if (blogs.has("region_name")) {
            location = blogs["region_name"].asString
            val index = location.indexOf(' ')
            if (index != -1) location = location.substring(index + 1)
        }
        // 提取内容
        val text = blogs["text"].asString
        // 提取数据
        val commentNum = blogs["comments_count"].asInt
        val likeNum = blogs["attitudes_count"].asInt
        val repostNum = blogs["reposts_count"].asInt
        if (blogs.has("retweeted_status")) {
            blogs = blogs["retweeted_status"].asJsonObject // 转发微博
        }
        // 图片微博
        val pictures = mutableListOf<RachelPreview>()
        if (blogs.has("pics")) {
            for (picItem in blogs.getAsJsonArray("pics")) {
                val pic = picItem.asJsonObject
                pictures += RachelPreview(pic["url"].asString, pic.getAsJsonObject("large")["url"].asString)
            }
        } else if (blogs.has("page_info")) {
            val pageInfo = blogs.getAsJsonObject("page_info")
            if (pageInfo["type"].asString == "video") {
                val urls = pageInfo.getAsJsonObject("urls")
                val videoUrl = if (urls.has("mp4_720p_mp4")) urls["mp4_720p_mp4"].asString
                else if (urls.has("mp4_hd_mp4")) urls["mp4_hd_mp4"].asString
                else urls["mp4_ld_mp4"].asString
                val videoPicUrl = pageInfo.getAsJsonObject("page_pic")["url"].asString
                pictures += RachelPreview(videoPicUrl, videoPicUrl, videoUrl)
            }
        }
        return Weibo(blogId, WeiboCommentUser(userId, userName, avatar, location), formattedTime, text, commentNum, likeNum, repostNum, pictures)
    }

    fun extractAllWeibo(uid: String, array: WeiboList) {
        try {
            val url = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=${WeiboContainer.weibo(uid)}"
            val json = Net.get(url).asJsonObject
            val cards = json.getAsJsonObject("data").getAsJsonArray("cards")
            for (item in cards) {
                try {
                    val card = item.asJsonObject
                    if (card["card_type"].asInt != 9) continue  // 非微博类型
                    array += extractWeibo(card)
                }
                catch (_: Exception) { }
            }
        }
        catch (_: Exception) { }
    }

    fun extractAllUserWeibo(weiboUsers: WeiboUserStorageList, array: WeiboList) {
        for (user in weiboUsers) extractAllWeibo(user.userId, array)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        array.sortWith { o1, o2 ->
            val dateTime1 = LocalDateTime.parse(o1.time, formatter)
            val dateTime2 = LocalDateTime.parse(o2.time, formatter)
            dateTime1.compareTo(dateTime2) * -1
        }
    }

    fun extractWeiboUser(uid: String): WeiboUser? =  try {
        val url = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid"
        val json = Net.get(url).asJsonObject
        val userInfo = json.getAsJsonObject("data").getAsJsonObject("userInfo")
        val userId = userInfo["id"].asString
        val name = userInfo["screen_name"].asString
        val avatar = userInfo["avatar_hd"].asString
        val background = userInfo["cover_image_phone"].asString
        val signature = userInfo["description"].asString
        val followNum = userInfo["follow_count"].asInt.toString()
        val fansNum = if (userInfo.has("followers_count_str"))
            userInfo["followers_count_str"].asString else userInfo["followers_count"].asString
        WeiboUser(userId, name, avatar, background, signature, followNum, fansNum)
    }
    catch (_: Exception) { null }

    fun extractWeiboUserAlbum(uid: String): WeiboAlbumList = try {
        val albums = mutableListOf<WeiboAlbum>()
        val albumUrl = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=${WeiboContainer.album(uid)}"
        val albumJson = Net.get(albumUrl).asJsonObject
        val cards = albumJson.getAsJsonObject("data").getAsJsonArray("cards")
        for (item1 in cards) {
            val card = item1.asJsonObject
            if (card["itemid"].asString.endsWith("albumeach")) {
                for (item2 in card.getAsJsonArray("card_group")) {
                    val album = item2.asJsonObject
                    if (album["card_type"].asInt == 8) {
                        val containerId = Uri.parse(album["scheme"].asString).getQueryParameter("containerid")!!
                        val title = album["title_sub"].asString
                        val num = album["desc1"].asString
                        val time = album["desc2"].asString
                        val pic = album["pic"].asString
                        albums += WeiboAlbum(containerId, title, num, time, pic)
                    }
                }
            }
        }
        albums
    }
    catch (_: Exception) { mutableListOf() }

    fun extractWeiboUserAlbumPics(containerId: String, page: Int, limit: Int, count: IntRef): List<RachelPreview> = try {
        val url = "https://m.weibo.cn/api/container/getSecond?containerid=${containerId}&count=${limit}&page=${page}"
        val json = Net.get(url).asJsonObject
        val data = json.getAsJsonObject("data")
        val cards = data.getAsJsonArray("cards")
        val pics = mutableListOf<RachelPreview>()
        for (item1 in cards) {
            val card = item1.asJsonObject
            for (item2 in card.getAsJsonArray("pics")) {
                val pic = item2.asJsonObject
                pics += RachelPreview(pic["pic_middle"].asString, pic["pic_ori"].asString)
            }
        }
        count.element = data["count"].asInt
        pics.take(limit)
    }
    catch (_: Exception) { mutableListOf() }

    private fun extractWeiboComment(card: JsonObject): WeiboComment {
        // 提取名称和头像
        val user = card.getAsJsonObject("user")
        val userId = user["id"].asString
        val userName = user["screen_name"].asString
        val avatar = user["avatar_hd"].asString
        // 提取时间
        val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        var formattedTime = "未知时间"
        try {
            val date = inputFormat.parse(card["created_at"].asString)
            formattedTime = outputFormat.format(date!!)
        } catch (_: Exception) { }
        // 提取IP
        val location = if (card.has("source")) card["source"].asString.removePrefix("来自") else "IP未知"
        // 提取内容
        val text = card["text"].asString
        return WeiboComment(WeiboCommentUser(userId, userName, avatar, location), formattedTime, text)
    }

    fun extractWeiboDetails(id: String, array: WeiboCommentList) {
        try {
            val url = "https://m.weibo.cn/comments/hotflow?id=${id}&mid=${id}"
            val json = Net.get(url).asJsonObject
            val cards = json.getAsJsonObject("data").getAsJsonArray("data")
            for (item in cards) {
                val card = item.asJsonObject
                val comment = extractWeiboComment(card)
                // 带图片
                if (card.has("pic")) {
                    val pic = card.getAsJsonObject("pic")
                    comment.pic = RachelPreview(pic["url"].asString, pic.getAsJsonObject("large")["url"].asString)
                }
                // 楼中楼
                val comments = card["comments"]
                if (comments.isJsonArray) {
                    val subComments = mutableListOf<WeiboComment>()
                    for (subCard in comments.asJsonArray) subComments += extractWeiboComment(subCard.asJsonObject)
                    comment.subComments = subComments
                }
                array += comment
            }
        }
        catch (_: Exception) { }
    }

    fun extractChaohua(sinceId: Long, array: WeiboList): Long {
        try {
            val url = "https://m.weibo.cn/api/container/getIndex?containerid=${WeiboContainer.CHAOHUA}&type=uid&value=2266537042&since_id=${sinceId}"
            val json = Net.get(url).asJsonObject
            val data = json.getAsJsonObject("data")
            val pageInfo = data.getAsJsonObject("pageInfo")
            val newSinceId = pageInfo["since_id"].asLong
            val cards = data.getAsJsonArray("cards")
            for (item in cards) {
                try {
                    val card = item.asJsonObject
                    val cardType = card["card_type"].asInt
                    if (cardType == 11) {
                        if (card.has("card_group")) {
                            for (subCard in card["card_group"].asJsonArray) array += extractWeibo(subCard.asJsonObject)
                        }
                    }
                    else if (cardType == 9) array += extractWeibo(card)
                }
                catch (_: Exception) { }
            }
            return newSinceId
        }
        catch (_: Exception) {
            return 0L
        }
    }
}