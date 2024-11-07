package com.yinlin.rachel.fragment

import com.google.android.material.tabs.TabLayoutMediator
import com.yinlin.rachel.databinding.FragmentMsgBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelFragmentPage
import com.yinlin.rachel.model.RachelPages

class FragmentMsg(pages: RachelPages) : RachelFragment<FragmentMsgBinding>(pages)  {
    override fun bindingClass() = FragmentMsgBinding::class.java

    override fun init() {
        v.viewpager.adapter = RachelFragmentPage.Adapter(pages.activity,
            arrayOf(
                FragmentMsgWeibo(pages),
                FragmentMsgChaohua(pages),
                FragmentMsgPhoto(pages),
            )
        )
        TabLayoutMediator(v.tab, v.viewpager) { tab, position ->
            tab.setText(arrayOf("微博", "超话", "美图")[position])
        }.attach()
    }
}