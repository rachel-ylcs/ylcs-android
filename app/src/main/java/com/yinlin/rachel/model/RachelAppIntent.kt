package com.yinlin.rachel.model

import android.content.Context
import android.content.Intent
import android.net.Uri

open class RachelAppIntent(val name: String, private val uri: String) {
    fun start(context: Context): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        true
    } catch (_: Exception) { false }

    class QQ(id: String) : RachelAppIntent("QQ", "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=${id}&card_type=person&source=qrcode")
    class QQGroup(id: String) : RachelAppIntent("QQ", "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=${id}&card_type=group&source=qrcode")
    class Taobao(shopId: String) : RachelAppIntent("淘宝", "taobao://shop.m.taobao.com/shop/shop_index.htm?shop_id=${shopId}")
    class Showstart(url: String) : RachelAppIntent("秀动", url)
}