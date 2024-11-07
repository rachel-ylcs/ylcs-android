package com.yinlin.rachel.fragment

import com.yinlin.rachel.databinding.FragmentMsgWeiboBinding
import com.yinlin.rachel.model.RachelFragmentPage
import com.yinlin.rachel.model.RachelPages

class FragmentMsgWeibo(pages: RachelPages) : RachelFragmentPage<FragmentMsgWeiboBinding>(pages) {
    override fun bindingClass() = FragmentMsgWeiboBinding::class.java
}