package com.yinlin.rachel.fragment

import com.yinlin.rachel.databinding.FragmentMeBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPages

class FragmentMe(pages: RachelPages) : RachelFragment<FragmentMeBinding>(pages)  {
    override fun bindingClass() = FragmentMeBinding::class.java
}