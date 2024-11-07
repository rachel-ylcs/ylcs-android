package com.yinlin.rachel.fragment;

import com.yinlin.rachel.databinding.FragmentMsgPhotoBinding
import com.yinlin.rachel.model.RachelFragmentPage
import com.yinlin.rachel.model.RachelPages

class FragmentMsgPhoto(pages: RachelPages) : RachelFragmentPage<FragmentMsgPhotoBinding>(pages) {
    override fun bindingClass() = FragmentMsgPhotoBinding::class.java

}
