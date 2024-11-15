package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentMeBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.pureColor
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentMe(pages: RachelPages) : RachelFragment<FragmentMeBinding>(pages)  {
    private val rilNet = RachelImageLoader(pages.context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

    override fun bindingClass() = FragmentMeBinding::class.java

    override fun init() {
        // 设置
        v.buttonSettings.rachelClick { pages.navigate(FragmentSettings(pages)) }

        // 下拉刷新
        v.container.setOnRefreshListener {
            if (Config.isLogin) { requestUserInfo() }
            else pages.navigate(FragmentLogin(pages))
            if (v.container.isRefreshing) v.container.finishRefresh()
        }
    }

    override fun update() {
        updateUserInfo(Config.user)
    }

    override fun quit() {

    }

    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            RachelMessage.ME_UPDATE_USER_INFO -> updateUserInfo(args[0] as User?)
            RachelMessage.ME_REQUEST_USER_INFO -> if (Config.isLogin) requestUserInfo()
            // TODO:
            // RachelMessage.ME_ADD_ACTIVITY -> addActivity(args[0] as User, args[1] as Calendar, args[2] as ShowActivity)
            else -> { }
        }
    }

    private fun updateUserInfo(user: User?) {
        if (user != null) {
            v.name.text = user.name
            v.label.setLabel(user.label, user.level)
            v.signature.text = user.signature
            v.level.text = user.level.toString()
            v.coin.text = user.coin.toString()
            v.avatar.load(rilNet, user.avatarPath, Config.cache_key_avatar)
            v.wall.load(rilNet, user.wallPath, Config.cache_key_wall)
        }
        else {
            v.name.text = pages.getResString(R.string.default_name)
            v.label.setDefaultLabel()
            v.signature.text = pages.getResString(R.string.default_signature)
            v.level.text = "1"
            v.coin.text = "0"
            v.avatar.pureColor = pages.getResColor(R.color.white)
            v.wall.pureColor = pages.getResColor(R.color.dark)
        }
    }

    @NewThread
    private fun requestUserInfo() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { API.UserAPI.getInfo(Config.token) }
            when (result.code) {
                API.Code.SUCCESS -> {
                    val user: User = result.fetch()
                    Config.user = user
                    updateUserInfo(user)
                }
                API.Code.UNAUTHORIZED -> {
                    tip(Tip.WARNING, result.msg)
                    Config.token = ""
                    Config.user = null
                    updateUserInfo(null)
                    pages.navigate(FragmentLogin(pages))
                }
                else -> tip(Tip.ERROR, result.msg)
            }
        }
    }
}