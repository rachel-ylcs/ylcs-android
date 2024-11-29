package com.yinlin.rachel.sheet

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.databinding.ItemLyricsEngineBinding
import com.yinlin.rachel.databinding.SheetLyricsEngineBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.tool.interceptScroll
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.model.engine.LyricsEngineFactory
import com.yinlin.rachel.model.engine.LyricsEngineInfo

class SheetLyricsEngine(fragment: FragmentMusic) : RachelSheet<SheetLyricsEngineBinding, FragmentMusic>(fragment, 0.8f) {
    class Adapter : RachelAdapter<ItemLyricsEngineBinding, LyricsEngineInfo>() {
        override fun bindingClass() = ItemLyricsEngineBinding::class.java
        init { setSource(LyricsEngineFactory.engineInfos) }

        override fun update(v: ItemLyricsEngineBinding, item: LyricsEngineInfo, position: Int) {
            v.name.text = item.name
            v.description.text = item.description
            v.icon.load(item.icon)
        }
    }

    override fun bindingClass() = SheetLyricsEngineBinding::class.java

    override fun init() {
        v.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            interceptScroll()
        }
    }
}