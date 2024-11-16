package com.yinlin.rachel.api

import com.google.gson.JsonObject
import com.yinlin.rachel.Net
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.data.weibo.WeiboComment
import com.yinlin.rachel.data.weibo.WeiboCommentList
import com.yinlin.rachel.data.weibo.WeiboList
import com.yinlin.rachel.data.weibo.WeiboUser
import com.yinlin.rachel.data.weibo.WeiboUserInfo
import com.yinlin.rachel.data.weibo.WeiboUserStorageMap
import com.yinlin.rachel.model.RachelPreview
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


object WeiboAPI {
    fun extractContainerId(uid: String): Array<String>? = try {
        val url = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid"
        val json = Net.get(url).asJsonObject
        val data = json.getAsJsonObject("data")
        val name = data.getAsJsonObject("userInfo")["screen_name"].asString
        val tabs = data.getAsJsonObject("tabsInfo").getAsJsonArray("tabs")
        var arr: Array<String>? = null
        for (item in tabs) {
            val tab = item.asJsonObject
            if (tab["title"].asString == "微博") {
                arr = arrayOf(uid, name, tab["containerid"].asString)
                break
            }
        }
        arr
    } catch (ignored: Exception) { null }

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
        } catch (ignored: Exception) { }
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
        return Weibo(blogId, WeiboUser(userId, userName, avatar, location), formattedTime, text, commentNum, likeNum, repostNum, pictures)
    }

    fun getWeiboUserInfo(uid: String): WeiboUserInfo? =  try {
        val url = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid"
        val json = Net.get(url).asJsonObject
        val userInfo = json.getAsJsonObject("data").getAsJsonObject("userInfo")
        val userId = userInfo["id"].asString
        val name = userInfo["screen_name"].asString
        val avatar = userInfo["avatar_hd"].asString
        val signature = userInfo["description"].asString
        val followNum = userInfo["follow_count"].asInt.toString()
        val fansNum = if (userInfo.has("followers_count_str"))
            userInfo["followers_count_str"].asString else userInfo["followers_count"].asString
        WeiboUserInfo(userId, name, avatar, signature, followNum, fansNum)
    }
    catch (ignored: Exception) { null }

    private fun getWeibo(uid: String, containerId: String, array: WeiboList) {
        try {
            val url = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=$containerId"
            val json = Net.get(url).asJsonObject
            val cards = json.getAsJsonObject("data").getAsJsonArray("cards")
            for (item in cards) {
                try {
                    val card = item.asJsonObject
                    if (card["card_type"].asInt != 9) continue  // 非微博类型
                    array += extractWeibo(card)
                }
                catch (ignored: Exception) { }
            }
        }
        catch (ignored: Exception) { }
    }

    fun getAllWeibo(weiboUsers: WeiboUserStorageMap, array: WeiboList) {
        for ((key, value) in weiboUsers) getWeibo(key, value.containerId, array)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        array.sortWith { o1, o2 ->
            val dateTime1 = LocalDateTime.parse(o1.time, formatter)
            val dateTime2 = LocalDateTime.parse(o2.time, formatter)
            dateTime1.compareTo(dateTime2) * -1
        }
    }

    private fun extractComment(card: JsonObject): WeiboComment {
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
        } catch (ignored: Exception) { }
        // 提取IP
        val location = if (card.has("source")) card["source"].asString.removePrefix("来自") else "IP未知"
        // 提取内容
        val text = card["text"].asString
        return WeiboComment(WeiboUser(userId, userName, avatar, location), formattedTime, text)
    }

    fun getDetails(id: String, array: WeiboCommentList) {
        try {
            val url = "https://m.weibo.cn/comments/hotflow?id=${id}&mid=${id}"
            val json = Net.get(url).asJsonObject
            val cards = json.getAsJsonObject("data").getAsJsonArray("data")
            for (item in cards) {
                val card = item.asJsonObject
                val comment = extractComment(card)
                // 带图片
                if (card.has("pic")) {
                    val pic = card.getAsJsonObject("pic")
                    comment.pic = RachelPreview(pic["url"].asString, pic.getAsJsonObject("large")["url"].asString)
                }
                // 楼中楼
                val comments = card["comments"]
                if (comments.isJsonArray) {
                    val subComments = mutableListOf<WeiboComment>()
                    for (subCard in comments.asJsonArray) subComments += extractComment(subCard.asJsonObject)
                    comment.subComments = subComments
                }
                array += comment
            }
        }
        catch (ignored: Exception) { }
    }

    fun getChaohua(sinceId: Long, array: WeiboList): Long {
        try {
            val url = "https://m.weibo.cn/api/container/getIndex?containerid=10080848e33cc4065cd57c5503c2419cdea983_-_sort_time&type=uid&value=2266537042&since_id=${sinceId}"
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
                catch (ignored: Exception) { }
            }
            return newSinceId
        }
        catch (ignored: Exception) {
            return 0L
        }
    }
}