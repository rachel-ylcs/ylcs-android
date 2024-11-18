package com.yinlin.rachel.fragment

import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.databinding.FragmentAboutBinding
import com.yinlin.rachel.model.RachelFragment

class FragmentAbout(main: MainActivity) : RachelFragment<FragmentAboutBinding>(main) {
    override fun bindingClass() = FragmentAboutBinding::class.java

    override fun back() = true
}