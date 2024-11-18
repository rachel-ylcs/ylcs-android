package com.yinlin.rachel.dialog

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.databinding.BottomDialogLyricsEngineBinding
import com.yinlin.rachel.databinding.ItemLyricsEngineBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.interceptScroll
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelBottomDialog
import com.yinlin.rachel.model.engine.LyricsEngineFactory
import com.yinlin.rachel.model.engine.LyricsEngineInfo

class BottomDialogLyricsEngine(fragment: FragmentMusic) : RachelBottomDialog<BottomDialogLyricsEngineBinding, FragmentMusic>(
    fragment, 0.8f, BottomDialogLyricsEngineBinding::class.java) {
    class Adapter(private val dialog: BottomDialogLyricsEngine) : RachelAdapter<ItemLyricsEngineBinding, LyricsEngineInfo>() {
        override fun bindingClass() = ItemLyricsEngineBinding::class.java
        init {
            setSource(LyricsEngineFactory.engineInfos)
        }

        override fun update(v: ItemLyricsEngineBinding, item: LyricsEngineInfo, position: Int) {
            v.name.text = item.name
            v.description.text = item.description
            v.icon.load(dialog.root.main.ril, item.icon)
        }
    }

    private val adapter = Adapter(this)

    override fun init() {
        v.list.apply {
            layoutManager = LinearLayoutManager(root.main)
            adapter = this@BottomDialogLyricsEngine.adapter
            interceptScroll()
        }
    }

    fun update(): BottomDialogLyricsEngine {
        v.list.scrollToPosition(0)
        return this
    }
}