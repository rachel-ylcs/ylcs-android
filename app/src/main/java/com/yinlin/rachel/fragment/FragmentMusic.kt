package com.yinlin.rachel.fragment

import com.yinlin.rachel.databinding.FragmentMusicBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPages

class FragmentMusic(pages: RachelPages) : RachelFragment<FragmentMusicBinding>(pages)  {
    override fun bindingClass() = FragmentMusicBinding::class.java
}