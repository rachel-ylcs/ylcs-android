package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Net
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.NetEaseCloudAPI
import com.yinlin.rachel.common.SilentDownloadListener
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.neteasecloud.CloudMusic
import com.yinlin.rachel.databinding.FragmentImportNeteaseCloudBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.writeRes
import com.yinlin.rachel.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.OutputStream

class FragmentImportNetEaseCloud(main: MainActivity, private val text: String, private val isShareUrl: Boolean = false) : RachelFragment<FragmentImportNeteaseCloudBinding>(main) {
    private var musicInfo: CloudMusic? = null
    private var lrcData: LrcData? = null

    override fun bindingClass() = FragmentImportNeteaseCloudBinding::class.java

    override fun init() {
        v.loading.rachelClick {
            if (!v.loading.loading) musicInfo?.let { downloadMusic(it) }
        }

        requestMusic(text, isShareUrl)
    }

    override fun back() = true

    @NewThread
    private fun requestMusic(text: String, isShareUrl: Boolean) {
        lifecycleScope.launch {
            v.loading.loading = true
            val id = if (isShareUrl) {
                withContext(Dispatchers.IO) { NetEaseCloudAPI.getMusicId(text) }
            } else text
            musicInfo = id?.let {
                withContext(Dispatchers.IO) { NetEaseCloudAPI.getMusicInfo(it) }
            }
            v.loading.loading = false
            v.loading.text = "导入"
            lrcData = musicInfo?.lyrics?.let { LrcData.parseLrcData(it) }
            if (lrcData != null && musicInfo != null) {
                v.name.text = musicInfo!!.name
                v.singer.text = musicInfo!!.singer
                v.time.text = "时长: ${musicInfo!!.time}"
                v.pic.load(musicInfo!!.pic)
                v.lyrics.text = lrcData!!.plainText
            }
            else {
                tip(Tip.WARNING, "解析失败")
                main.pop()
            }
        }
    }

    @NewThread
    private fun downloadMusic(music: CloudMusic) {
        lifecycleScope.launch {
            val loading = main.loading
            // 生成 Info
            val musicId = "NEC${music.id}"
            val info = MusicInfo("1.0", "网易云音乐", musicId, music.name, music.singer,
                "未知", "未知", "未知", bgd = false, video = false,
                chorus = mutableListOf(), lyrics = mutableMapOf("line" to mutableListOf("")), lrcData)
            withContext(Dispatchers.IO) {
                // 元数据
                info.rewrite()
                // 壁纸
                info.bgsPath.writeRes(main, R.raw.img_default_bgs)
                // 歌词
                info.defaultLrcPath.writeText(music.lyrics)
                // 封面
                Net.download(music.pic, listener = object : SilentDownloadListener() {
                    override suspend fun onPrepare(url: String): OutputStream {
                        return FileOutputStream(info.recordPath)
                    }
                })
                // 音频
                Net.download(music.mp3Url, listener = object : SilentDownloadListener() {
                    override suspend fun onPrepare(url: String): OutputStream {
                        return FileOutputStream(info.audioPath)
                    }
                })
            }
            loading.dismiss()
            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_NOTIFY_ADD_MUSIC, listOf(musicId))
            tip(Tip.SUCCESS, "导入成功")
        }
    }
}