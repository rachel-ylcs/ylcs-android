package com.yinlin.rachel.fragment


import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentWorldBinding
import com.yinlin.rachel.model.RachelFragment

@Layout(FragmentWorldBinding::class)
class FragmentWorld(main: MainActivity) : RachelFragment<FragmentWorldBinding>(main)  {
    override fun init() {

    }

    override fun back() = BackState.HOME
}