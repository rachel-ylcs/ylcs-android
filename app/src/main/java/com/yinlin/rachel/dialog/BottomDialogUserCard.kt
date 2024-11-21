package com.yinlin.rachel.dialog

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.king.zxing.util.CodeUtils
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.BottomDialogUserCardBinding
import com.yinlin.rachel.fragment.FragmentMe
import com.yinlin.rachel.model.RachelBottomDialog
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.toDP

class BottomDialogUserCard(fragment: FragmentMe) : RachelBottomDialog<BottomDialogUserCardBinding, FragmentMe>(
    fragment, 0.9f, BottomDialogUserCardBinding::class.java) {
    private val main = fragment.main

    fun update(user: User): BottomDialogUserCard {
        v.id.text = user.name
        v.avatar.load(user.avatarPath, Config.cache_key_avatar)
        v.qrcode.setImageBitmap(CodeUtils.createQRCode(
            "rachel://yinlin/openProfile?uid=${user.uid}",
            200.toDP(main),
            AppCompatResources.getDrawable(main, R.mipmap.icon)!!.toBitmap())
        )
        return this
    }
}