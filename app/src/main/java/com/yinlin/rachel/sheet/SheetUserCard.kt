package com.yinlin.rachel.sheet

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.king.zxing.util.CodeUtils
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.SheetUserCardBinding
import com.yinlin.rachel.fragment.FragmentMe
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.toDP

class SheetUserCard(fragment: FragmentMe, private val user: User) : RachelSheet<SheetUserCardBinding, FragmentMe>(fragment, 0.6f) {
    override fun bindingClass() = SheetUserCardBinding::class.java

    override fun init() {
        v.id.text = user.name
        v.avatar.load(user.avatarPath, Config.cache_key_avatar)
        v.qrcode.setImageBitmap(
            CodeUtils.createQRCode(
            "rachel://yinlin/openProfile?uid=${user.uid}",
            150.toDP(context),
            AppCompatResources.getDrawable(context, R.mipmap.icon)!!.toBitmap())
        )
    }
}