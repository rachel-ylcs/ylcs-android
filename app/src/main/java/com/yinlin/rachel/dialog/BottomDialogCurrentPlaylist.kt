package com.yinlin.rachel.dialog

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LoadMusicPreview
import com.yinlin.rachel.data.music.LoadMusicPreviewList
import com.yinlin.rachel.databinding.BottomDialogCurrentPlaylistBinding
import com.yinlin.rachel.databinding.ItemMusicLineBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.interceptScroll
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelBottomDialog
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.strikethrough
import com.yinlin.rachel.textColor

class BottomDialogCurrentPlaylist(fragment: FragmentMusic) : RachelBottomDialog<BottomDialogCurrentPlaylistBinding, FragmentMusic>(
    fragment, 0.6f, BottomDialogCurrentPlaylistBinding::class.java) {
    class Adapter(private val dialog: BottomDialogCurrentPlaylist) : RachelAdapter<ItemMusicLineBinding, LoadMusicPreview>() {
        private val main = dialog.root.main
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
            }
            v.singer.apply {
                text = item.singer
                textColor = if (item.isPlaying) playingColor else normalSingerColor
            }
        }

        override fun onItemClicked(v: ItemMusicLineBinding, item: LoadMusicPreview, position: Int) {
            if (!item.isDeleted) {
                dialog.hide()
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_GOTO_MUSIC, item.id)
            }
            else dialog.tip(Tip.WARNING, main.rs(R.string.no_audio_source))
        }
    }

    private var adapter = Adapter(this)

    override fun init() {
        v.buttonStop.rachelClick {
            hide()
            root.main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
        }

        v.list.apply {
            layoutManager = LinearLayoutManager(root.main)
            recycledViewPool.setMaxRecycledViews(0, 15)
            adapter = this@BottomDialogCurrentPlaylist.adapter
            interceptScroll()
        }
    }

    fun update(name: String, items: LoadMusicPreviewList): BottomDialogCurrentPlaylist {
        v.title.text = name
        adapter.setSource(items)
        adapter.notifySource()
        var currentIndex = 0
        for ((index, item) in items.withIndex()) {
            if (item.isPlaying) {
                currentIndex = index
                break
            }
        }
        v.list.scrollToPosition(currentIndex)
        return this
    }
}