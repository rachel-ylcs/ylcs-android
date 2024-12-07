package com.yinlin.rachel.fragment

import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentPrivacyPolicyBinding
import com.yinlin.rachel.model.RachelFragment

class FragmentPrivacyPolicy(main: MainActivity) : RachelFragment<FragmentPrivacyPolicyBinding>(main) {
    override fun bindingClass() = FragmentPrivacyPolicyBinding::class.java

    override fun back() = BackState.POP
}