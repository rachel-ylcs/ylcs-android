package com.yinlin.rachel.sheet

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.annotation.SheetLayout
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LyricsInfo
import com.yinlin.rachel.data.music.LyricsInfoList
import com.yinlin.rachel.databinding.ItemLyricsInfoBinding
import com.yinlin.rachel.databinding.SheetLyricsInfoBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.tool.interceptScroll
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.textColor

@SheetLayout(SheetLyricsInfoBinding::class, 0.6f)
class SheetLyricsInfo(fragment: FragmentMusic, private val infos: LyricsInfoList)
    : RachelSheet<SheetLyricsInfoBinding, FragmentMusic>(fragment) {
    @Layout(ItemLyricsInfoBinding::class)
    class Adapter(private val sheet: SheetLyricsInfo) : RachelAdapter<ItemLyricsInfoBinding, LyricsInfo>() {
        private val main = sheet.fragment.main
        private val unlockedString = main.rs(R.string.unlocked)
        private val lockedString = main.rs(R.string.locked)
        private val unlockedColor = main.rc(R.color.sea_green)
        private val lockedColor = main.rc(R.color.red)

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
            sheet.dismiss()
            if (item.available) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_USE_LYRICS_ENGINE, item.engineName, item.name)
            else sheet.tip(Tip.WARNING, "未解锁该歌词引擎")
        }
    }

    private val mAdapter = Adapter(this)

    override fun init() {
        v.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            interceptScroll()
        }

        mAdapter.setSource(infos)
        mAdapter.notifySource()
    }
}