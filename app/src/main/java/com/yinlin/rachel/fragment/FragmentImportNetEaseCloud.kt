package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Net
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.api.NetEaseCloudAPI
import com.yinlin.rachel.common.SilentDownloadListener
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.PlayingMusicPreviewList
import com.yinlin.rachel.data.neteasecloud.CloudMusic
import com.yinlin.rachel.databinding.FragmentImportNeteaseCloudBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.tool.withIO
import com.yinlin.rachel.tool.writeRes
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.OutputStream

@Layout(FragmentImportNeteaseCloudBinding::class)
class FragmentImportNetEaseCloud(main: MainActivity, private val text: String, private val isShareUrl: Boolean = false) : RachelFragment<FragmentImportNeteaseCloudBinding>(main) {
    private var musicInfo: CloudMusic? = null
    private var lrcData: LrcData? = null

    override fun init() {
        v.loading.rachelClick {
            if (!v.loading.loading) musicInfo?.let { downloadMusic(it) }
        }

        requestMusic(text, isShareUrl)
    }

    override fun back() = BackState.POP

    @IOThread
    private fun requestMusic(text: String, isShareUrl: Boolean) {
        lifecycleScope.launch {
            v.loading.loading = true
            val id = if (isShareUrl) { withIO { NetEaseCloudAPI.getMusicId(text) } } else text
            musicInfo = id?.let { withIO { NetEaseCloudAPI.getMusicInfo(it) } }
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

    @IOThread
    private fun downloadMusic(music: CloudMusic) {
        // 生成 musicId
        val musicId = "NEC${music.id}-${music.name}"
        // 如果导入的歌曲正在播放则只能停止播放器
        val playlist = main.sendMessageForResult<PlayingMusicPreviewList>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_PLAYLIST_PREVIEW)!!
        if (playlist.indexOfFirst { it.id == musicId } != -1) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
        // 生成 Info
        val info = MusicInfo("1.0", "网易云音乐", musicId, music.name, music.singer,
            "未知", "未知", "未知", bgd = false, video = false,
            chorus = mutableListOf(), lyrics = mutableMapOf("line" to mutableListOf("")), lrcData)
        val loading = main.loading
        startIOWithResult({
            // 元数据
            info.rewrite()
            // 壁纸
            info.bgsPath.writeRes(main, R.raw.img_default_bgs)
            // 歌词
            info.defaultLrcPath.writeText(music.lyrics)
            // 封面
            Net.download(music.pic, mapOf("User-Agent" to "Mozilla/5.0"), listener = object : SilentDownloadListener() {
                override suspend fun onPrepare(url: String): OutputStream {
                    return FileOutputStream(info.recordPath)
                }
            })
            // 音频
            Net.download(music.mp3Url, mapOf("User-Agent" to "Mozilla/5.0"), listener = object : SilentDownloadListener() {
                override suspend fun onPrepare(url: String): OutputStream {
                    return FileOutputStream(info.audioPath)
                }
            })
        }) {
            loading.dismiss()
            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_NOTIFY_ADD_MUSIC, listOf(musicId))
            tip(Tip.SUCCESS, "导入成功")
        }
    }
}