package com.yinlin.rachel.fragment

import com.yinlin.rachel.databinding.FragmentMsgBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.page.PageChaohua
import com.yinlin.rachel.page.PagePhoto
import com.yinlin.rachel.page.PageWeibo

class FragmentMsg(pages: RachelPages) : RachelFragment<FragmentMsgBinding>(pages)  {
    override fun bindingClass() = FragmentMsgBinding::class.java

    override fun init() {
        v.tab.bindViewPager(v.viewpager, arrayOf("微博", "超话", "美图"), arrayOf(
            PageWeibo(this),
            PageChaohua(this),
            PagePhoto(this)
        ))
    }

    override fun back(): Boolean = (v.viewpager.adapter as RachelViewPage.Adapter).back(v.viewpager.currentItem)
}