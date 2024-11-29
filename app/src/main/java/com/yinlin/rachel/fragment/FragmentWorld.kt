package com.yinlin.rachel.fragment


import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentWorldBinding
import com.yinlin.rachel.model.RachelFragment

class FragmentWorld(main: MainActivity) : RachelFragment<FragmentWorldBinding>(main)  {
    override fun bindingClass() = FragmentWorldBinding::class.java

    override fun init() {

    }

    override fun back() = BackState.HOME
}