package com.yinlin.rachel.fragment

import com.yinlin.rachel.databinding.FragmentAboutBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPages

class FragmentAbout(pages: RachelPages) : RachelFragment<FragmentAboutBinding>(pages) {
    override fun bindingClass() = FragmentAboutBinding::class.java

    override fun back() = true
}