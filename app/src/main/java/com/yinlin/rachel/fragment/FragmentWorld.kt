package com.yinlin.rachel.fragment

import com.yinlin.rachel.databinding.FragmentWorldBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPages

class FragmentWorld(pages: RachelPages) : RachelFragment<FragmentWorldBinding>(pages)  {
    override fun bindingClass() = FragmentWorldBinding::class.java
}