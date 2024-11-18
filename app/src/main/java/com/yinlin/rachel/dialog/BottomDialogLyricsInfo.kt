package com.yinlin.rachel.dialog

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LyricsInfo
import com.yinlin.rachel.data.music.LyricsInfoList
import com.yinlin.rachel.databinding.BottomDialogLyricsInfoBinding
import com.yinlin.rachel.databinding.ItemLyricsInfoBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.interceptScroll
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelBottomDialog
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.textColor


class BottomDialogLyricsInfo(fragment: FragmentMusic) : RachelBottomDialog<BottomDialogLyricsInfoBinding, FragmentMusic>(
    fragment, 0.5f, BottomDialogLyricsInfoBinding::class.java) {
    class Adapter(private val dialog: BottomDialogLyricsInfo) : RachelAdapter<ItemLyricsInfoBinding, LyricsInfo>() {
        private val main = dialog.root.main
        private val unlockedString = main.rs(R.string.unlocked)
        private val lockedString = main.rs(R.string.locked)
        private val unlockedColor = main.rc(R.color.sea_green)
        private val lockedColor = main.rc(R.color.red)

        override fun bindingClass() = ItemLyricsInfoBinding::class.java

        override fun update(v: ItemLyricsInfoBinding, item: LyricsInfo, position: Int) {
            v.engineName.text = item.engineName
            v.name.text = item.name
            if (item.available) {
                v.available.text = unlockedString
                v.available.textColor = unlockedColor
            } else {
                v.available.text = lockedString
                v.available.textColor = lockedColor
            }
        }

        override fun onItemClicked(v: ItemLyricsInfoBinding, item: LyricsInfo, position: Int) {
            dialog.hide()
            if (item.available) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_USE_LYRICS_ENGINE, item.engineName, item.name)
            else dialog.tip(Tip.WARNING, "未解锁该歌词引擎")
        }
    }

    private val adapter = Adapter(this)

    override fun init() {
        v.list.apply {
            layoutManager = LinearLayoutManager(root.main)
            adapter = this@BottomDialogLyricsInfo.adapter
            interceptScroll()
        }
    }

    fun update(engineNames: LyricsInfoList): BottomDialogLyricsInfo {
        v.list.scrollToPosition(0)
        adapter.setSource(engineNames)
        adapter.notifySource()
        return this
    }
}