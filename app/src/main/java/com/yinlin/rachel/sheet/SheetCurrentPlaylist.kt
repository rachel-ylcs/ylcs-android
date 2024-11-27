package com.yinlin.rachel.sheet

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LoadMusicPreview
import com.yinlin.rachel.data.music.LoadMusicPreviewList
import com.yinlin.rachel.databinding.ItemMusicLineBinding
import com.yinlin.rachel.databinding.SheetCurrentPlaylistBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.interceptScroll
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.strikethrough
import com.yinlin.rachel.textColor

class SheetCurrentPlaylist(fragment: FragmentMusic, private val data: LoadMusicPreviewList)
    : RachelSheet<SheetCurrentPlaylistBinding, FragmentMusic>(fragment, 0.6f) {
    class Adapter(private val sheet: SheetCurrentPlaylist) : RachelAdapter<ItemMusicLineBinding, LoadMusicPreview>() {
        private val main = sheet.fragment.main
        private val normalColor = main.rc(R.color.black)
        private val playingColor = main.rc(R.color.steel_blue)
        private val deletedColor = main.rc(R.color.red)
        private val normalSingerColor = main.rc(R.color.gray)

        override fun bindingClass() = ItemMusicLineBinding::class.java

        override fun update(v: ItemMusicLineBinding, item: LoadMusicPreview, position: Int) {
            v.name.apply {
                text = item.name
                textColor = if (item.isDeleted) deletedColor else if (item.isPlaying) playingColor else normalColor
                strikethrough = item.isDeleted
                paint.isFakeBoldText = item.isPlaying
            }
            v.singer.apply {
                text = item.singer
                textColor = if (item.isPlaying) playingColor else normalSingerColor
                paint.isFakeBoldText = item.isPlaying
            }
        }

        override fun onItemClicked(v: ItemMusicLineBinding, item: LoadMusicPreview, position: Int) {
            if (!item.isDeleted) {
                sheet.dismiss()
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_GOTO_MUSIC, item.id)
            }
            else sheet.tip(Tip.WARNING, main.rs(R.string.no_audio_source))
        }
    }

    override fun bindingClass() = SheetCurrentPlaylistBinding::class.java

    override fun init() {
        v.buttonStop.rachelClick {
            dismiss()
            fragment.main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
        }

        v.list.apply {
            layoutManager = LinearLayoutManager(context)
            recycledViewPool.setMaxRecycledViews(0, 15)
            adapter = Adapter(this@SheetCurrentPlaylist).apply {
                setSource(data.items)
                notifySource()
            }
            interceptScroll()
        }

        val items = data.items
        v.title.text = "${data.name} / ${items.size}首"
        val currentIndex = items.indexOfFirst { it.isPlaying }
        if (currentIndex != -1) v.list.scrollToPosition(currentIndex)
    }
}