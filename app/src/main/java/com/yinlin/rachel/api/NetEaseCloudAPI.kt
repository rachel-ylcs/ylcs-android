package com.yinlin.rachel.api

import com.yinlin.rachel.Net
import com.yinlin.rachel.data.neteasecloud.CloudMusic
import com.yinlin.rachel.data.neteasecloud.CloudMusicPreview
import com.yinlin.rachel.data.neteasecloud.CloudMusicPreviewList
import com.yinlin.rachel.timeString
import java.net.URLEncoder


object NetEaseCloudAPI {
    fun getMusicId(shareUrl: String): String? = try {
        val result = Regex("https?://\\S+").find(shareUrl)
        result?.value?.let { url ->
            Net.get(url) { it.request.url.queryParameter("id")?.ifEmpty { null } }
        }
    }
    catch (_: Exception) { null }

    fun getMusicInfo(id: String): CloudMusic? = try {
        val url1 = "https://music.163.com/api/song/detail?id=${id}&ids=[${id}]"
        val song = Net.get(url1).asJsonObject["songs"].asJsonArray[0].asJsonObject
        val name = song["name"].asString
        val artists = mutableListOf<String>()
        for (item in song.getAsJsonArray("artists")) artists += item.asJsonObject["name"].asString
        val singer = artists.joinToString(",")
        val time = song["duration"].asLong.timeString
        val pic = song["album"].asJsonObject["picUrl"].asString
        val url2 = "https://music.163.com/api/song/lyric?id=${id}&lv=1"
        val lyrics = Net.get(url2).asJsonObject["lrc"].asJsonObject["lyric"].asString
        val mp3Url = "https://music.163.com/song/media/outer/url?id=${id}"
        CloudMusic(id, name, singer, time, pic, lyrics, mp3Url)
    }
    catch (_: Exception) { null }

    fun searchMusic(key: String): CloudMusicPreviewList = try {
        val list = mutableListOf<CloudMusicPreview>()
        val encodeKey = try { URLEncoder.encode(key, "UTF-8") } catch (_: Exception) { key }
        val url = "https://music.163.com/api/search/get/web?s=${encodeKey}&type=1"
        val json = Net.get(url).asJsonObject
        for (item1 in json.getAsJsonObject("result").getAsJsonArray("songs")) {
            val song = item1.asJsonObject
            val id = song["id"].asString
            val name = song["name"].asString
            val artists = mutableListOf<String>()
            for (item2 in song.getAsJsonArray("artists")) artists += item2.asJsonObject["name"].asString
            val singer = artists.joinToString(",")
            val time = song["duration"].asLong.timeString
            list += CloudMusicPreview(id, name, singer, time)
        }
        list
    }
    catch (_: Exception) { emptyList() }
}