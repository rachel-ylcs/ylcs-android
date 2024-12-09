package com.yinlin.rachel.fragment

import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentPrivacyPolicyBinding
import com.yinlin.rachel.model.RachelFragment

@Layout(FragmentPrivacyPolicyBinding::class)
class FragmentPrivacyPolicy(main: MainActivity) : RachelFragment<FragmentPrivacyPolicyBinding>(main) {
    override fun back() = BackState.POP
}