package com.yinlin.rachel.dialog

import androidx.appcompat.content.res.AppCompatResources
import com.yinlin.rachel.R
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.databinding.BottomDialogMusicInfoBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.interceptScroll
import com.yinlin.rachel.model.RachelBottomDialog
import com.yinlin.rachel.model.RachelImageLoader.load

class BottomDialogMusicInfo(fragment: FragmentMusic) : RachelBottomDialog<BottomDialogMusicInfoBinding, FragmentMusic>(
    fragment, 0.7f, BottomDialogMusicInfoBinding::class.java) {
    private var currentMusicInfo: MusicInfo? = null
    private val drawableYes = AppCompatResources.getDrawable(fragment.main, R.drawable.icon_yes)
    private val drawableNo = AppCompatResources.getDrawable(fragment.main, R.drawable.icon_no)

    override fun init() {
        v.lyricsContainer.interceptScroll()
    }

    fun update(musicInfo: MusicInfo): BottomDialogMusicInfo {
        if (currentMusicInfo != musicInfo) {
            currentMusicInfo = musicInfo
            v.name.text = musicInfo.name
            v.version.text = "v ${musicInfo.version}"
            v.id.text = "ID: ${musicInfo.id}"
            v.pic.load(musicInfo.recordPath)
            v.singer.text = "演唱: ${musicInfo.singer}"
            v.lyricist.text = "作词: ${musicInfo.lyricist}"
            v.composer.text = "作曲: ${musicInfo.composer}"
            v.album.text = "专辑分类: ${musicInfo.album}"
            v.author.text = "MOD作者: ${musicInfo.author}"
            v.an.setImageDrawable(if (musicInfo.bgd) drawableYes else drawableNo)
            v.mv.setImageDrawable(if (musicInfo.video) drawableYes else drawableNo)
            v.lyrics.text = musicInfo.lyricsText
            v.lyricsContainer.scrollTo(0, 0)
        }
        return this
    }
}