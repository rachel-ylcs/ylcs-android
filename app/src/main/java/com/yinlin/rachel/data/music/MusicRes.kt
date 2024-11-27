package com.yinlin.rachel.data.music

import androidx.annotation.ColorRes
import com.yinlin.rachel.R
import com.yinlin.rachel.fileSizeString
import java.io.File

data class MusicRes(
    val id: Type,
    val name: String,
    val ext: String,
    val description: String,
    val isBasic: Boolean,
    @ColorRes val color: Int,
    val fileSize: String = "0 B",
) {
    enum class Type {
        UNKNOWN, // 未知资源
        INFO, // 元数据信息
        AUDIO, // 音频
        RECORD, // CD封面
        BGS, // 静态背景
        DEFAULT_LRC, // 默认歌词
        VIDEO, // 视频 PV
        BGD, // 动态背景
        LRC, // LRC歌词
        PAG, // PAG动效
    }

    companion object {
        const val INFO_NAME = ".json"
        const val AUDIO_NAME = ".flac"
        const val RECORD_NAME = "_record.webp"
        const val BGS_NAME = "_bgs.webp"
        const val DEFAULT_LRC_NAME = ".lrc"
        const val VIDEO_NAME = ".mp4"
        const val BGD_NAME = "_bgd.webp"

        private val Unknown = MusicRes(Type.UNKNOWN, "", "", "未知资源", false, R.color.music_res_unknown)
        private val Info = MusicRes(Type.INFO, INFO_NAME, "json", "元数据信息", true, R.color.music_res_info)
        private val Audio = MusicRes(Type.AUDIO, AUDIO_NAME, "flac", "音频", true, R.color.music_res_audio)
        private val Record = MusicRes(Type.RECORD, RECORD_NAME, "webp", "封面", true, R.color.music_res_record)
        private val Bgs = MusicRes(Type.BGS, BGS_NAME, "webp", "静态壁纸", true, R.color.music_res_bgs)
        private val DefaultLrc = MusicRes(Type.DEFAULT_LRC, DEFAULT_LRC_NAME, "lrc", "默认歌词", true, R.color.music_res_default_lrc)
        private val Video = MusicRes(Type.VIDEO, VIDEO_NAME, "mp4", "视频/PV", false, R.color.music_res_video)
        private val Bgd = MusicRes(Type.BGD, BGD_NAME, "webp", "动画壁纸", false, R.color.music_res_bgd)
        private val Lrc = MusicRes(Type.LRC, "", "lrc", "逐行歌词", false, R.color.music_res_lrc)
        private val Pag = MusicRes(Type.PAG, "", "pag", "动态歌词", false, R.color.music_res_pag)

        private val RES_KNOWN_MAP = mapOf(
            Info.name to Info,
            Audio.name to Audio,
            Record.name to Record,
            Bgs.name to Bgs,
            DefaultLrc.name to DefaultLrc,
            Video.name to Video,
            Bgd.name to Bgd,
        )

        private val RES_EXT_MAP = mapOf(
            Lrc.ext to Lrc,
            Pag.ext to Pag
        )

        fun checkResFile(id: String, file: File): Boolean {
            if (!file.isFile) return false
            val name = file.name
            if (!name.startsWith(id)) return false
            val resName = name.removePrefix(id)
            if (resName.indexOf('.') == -1) return false
            if (!resName.startsWith('.') && !resName.startsWith('_')) return false
            return file.extension.isNotEmpty()
        }

        fun parse(id: String, file: File): MusicRes {
            val fs = file.fileSizeString
            val name = file.name.removePrefix(id)
            val ext = file.extension
            val resBasicInfo = RES_KNOWN_MAP[name]
            if (resBasicInfo != null) return resBasicInfo.clone(fs)
            val resInfo = RES_EXT_MAP[ext] ?: Unknown
            return MusicRes(resInfo.id, name, ext, resInfo.description, resInfo.isBasic, resInfo.color, fs)
        }
    }

    fun clone(fs: String) = MusicRes(id, name, ext, description, isBasic, color, fs)
}