package com.yinlin.rachel.sheet

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.R
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.PlayingMusicPreview
import com.yinlin.rachel.data.music.PlayingMusicPreviewList
import com.yinlin.rachel.databinding.ItemMusicLineBinding
import com.yinlin.rachel.databinding.SheetCurrentPlaylistBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.tool.interceptScroll
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.textColor

class SheetCurrentPlaylist(
    fragment: FragmentMusic,
    private val playlistName: String,
    private val data: PlayingMusicPreviewList
) : RachelSheet<SheetCurrentPlaylistBinding, FragmentMusic>(fragment, 0.6f) {
    class Adapter(private val sheet: SheetCurrentPlaylist) : RachelAdapter<ItemMusicLineBinding, PlayingMusicPreview>() {
        private val main = sheet.fragment.main
        private val normalColor = main.rc(R.color.black)
        private val playingColor = main.rc(R.color.steel_blue)
        private val normalSingerColor = main.rc(R.color.gray)

        override fun bindingClass() = ItemMusicLineBinding::class.java

        override fun update(v: ItemMusicLineBinding, item: PlayingMusicPreview, position: Int) {
            v.name.apply {
                text = item.name
                textColor = if (item.isPlaying) playingColor else normalColor
                paint.isFakeBoldText = item.isPlaying
            }
            v.singer.apply {
                text = item.singer
                textColor = if (item.isPlaying) playingColor else normalSingerColor
                paint.isFakeBoldText = item.isPlaying
            }
        }

        override fun onItemClicked(v: ItemMusicLineBinding, item: PlayingMusicPreview, position: Int) {
            sheet.dismiss()
            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_GOTO_MUSIC, item.id)
        }
    }

    private val mAdapter = Adapter(this)

    override fun bindingClass() = SheetCurrentPlaylistBinding::class.java

    override fun init() {
        v.buttonStop.rachelClick {
            dismiss()
            fragment.main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
        }

        v.list.apply {
            layoutManager = LinearLayoutManager(context)
            recycledViewPool.setMaxRecycledViews(0, 15)
            adapter = mAdapter
            interceptScroll()
        }

        mAdapter.setSource(data)
        mAdapter.notifySource()

        v.title.text = "$playlistName - ${data.size}首"
        val currentIndex = data.indexOfFirst { it.isPlaying }
        if (currentIndex != -1) v.list.scrollToPosition(currentIndex)
    }
}