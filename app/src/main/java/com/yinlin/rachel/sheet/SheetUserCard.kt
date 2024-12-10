package com.yinlin.rachel.sheet

import com.king.zxing.util.CodeUtils
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.SheetLayout
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.SheetUserCardBinding
import com.yinlin.rachel.fragment.FragmentMe
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.tool.rb
import com.yinlin.rachel.tool.toDP

@SheetLayout(SheetUserCardBinding::class, 0.6f)
class SheetUserCard(fragment: FragmentMe, private val user: User) : RachelSheet<SheetUserCardBinding, FragmentMe>(fragment) {
    override fun init() {
        v.id.text = user.name
        v.avatar.load(user.avatarPath, Config.cache_key_avatar)
        v.qrcode.setImageBitmap(
            CodeUtils.createQRCode(
            "rachel://yinlin/openProfile?uid=${user.uid}",
            150.toDP(context),
            context.rb(R.mipmap.icon))
        )
    }
}