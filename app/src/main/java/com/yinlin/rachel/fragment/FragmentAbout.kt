package com.yinlin.rachel.fragment

import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentAboutBinding
import com.yinlin.rachel.model.RachelFragment

@Layout(FragmentAboutBinding::class)
class FragmentAbout(main: MainActivity) : RachelFragment<FragmentAboutBinding>(main) {
    override fun back() = BackState.POP
}