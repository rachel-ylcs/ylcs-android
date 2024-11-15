package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.databinding.FragmentWeiboUserBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentWeiboUser(pages: RachelPages, private val weiboUserId: String) : RachelFragment<FragmentWeiboUserBinding>(pages) {
    private val rilNet = RachelImageLoader(pages.context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

    override fun bindingClass() = FragmentWeiboUserBinding::class.java

    override fun init() {
        v.add.rachelClick {
            val weiboUserStorage: WeiboUserStorage? = Config.weibo_users[weiboUserId]
            if (weiboUserStorage != null) tip(Tip.WARNING, "${weiboUserStorage.name} 已存在")
            else addWeiboUser(weiboUserId)
        }

        requestUserInfo()
    }

    override fun back() = true

    @NewThread
    fun requestUserInfo() {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val userInfo = withContext(Dispatchers.IO) {
                val userInfo = WeiboAPI.getWeiboUserInfo(weiboUserId)
                withContext(Dispatchers.Main) { loading.dismiss() }
                userInfo
            }
            if (userInfo != null) {
                v.name.text = userInfo.name
                v.avatar.load(rilNet, userInfo.avatar, Config.cache_daily_pic)
                v.signature.text = userInfo.signature
                v.follow.text = "关注 ${userInfo.followNum}"
                v.fans.text = "粉丝 ${userInfo.fansNum}"
            }
            else {
                pages.pop()
                tip(Tip.ERROR, "用户不存在")
            }
        }
    }

    // 添加微博用户
    @NewThread
    private fun addWeiboUser(uid: String) {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val result = withContext(Dispatchers.IO) {
                val result = WeiboAPI.extractContainerId(uid)
                withContext(Dispatchers.Main) { loading.dismiss() }
                result
            }
            if (result != null) {
                val weiboUsers = Config.weibo_users
                weiboUsers[result[0]] = WeiboUserStorage(result[1], result[2])
                Config.weibo_users = weiboUsers
                tip(Tip.SUCCESS, "添加成功")
            }
            else tip(Tip.ERROR, "解析微博用户失败")
        }
    }
}