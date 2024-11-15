package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.data.weibo.names
import com.yinlin.rachel.databinding.FragmentSettingsBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelPictureSelector
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.pureColor
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentSettings(pages: RachelPages) : RachelFragment<FragmentSettingsBinding>(pages) {
    private val rilNet = RachelImageLoader(pages.context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

    override fun bindingClass() = FragmentSettingsBinding::class.java

    override fun init() {
        /*    ----    账号设置    ----    */

        // 更换头像
        v.avatar.rachelClick {
            if (Config.isLogin) RachelPictureSelector.single(pages.context, 256, 256, true) { updateAvatar(it) }
            else tip(Tip.WARNING, "请先登录")
        }

        // 退出登录
        v.logoff.rachelClick {
            if (Config.token.isNotEmpty()) { RachelDialog.confirm(pages.context, content="是否退出登录") { logoff() } }
            else tip(Tip.WARNING, "请先登录")
        }

        /*    ----    个性化设置    ----    */

        /*    ----    资讯设置    ----    */

        // 添加微博用户
        v.weibo.rachelClick {
            RachelDialog.input(pages.context, "请输入微博用户的uid(非昵称)", 20) {
                val weiboUserStorage: WeiboUserStorage? = Config.weibo_users[it]
                if (weiboUserStorage != null) tip(Tip.WARNING, "${weiboUserStorage.name} 已存在")
                else addWeiboUser(it)
            }
        }
        // 删除微博用户
        v.weiboList.listener = { index, text ->
            RachelDialog.confirm(pages.context, content="是否删除此微博用户") {
                val weiboUsers = Config.weibo_users
                weiboUsers.entries.removeIf { entry -> entry.value.name == text }
                Config.weibo_users = weiboUsers
                v.weiboList.removeTag(index)
            }
        }

        /*    ----    听歌设置    ----    */

        /*    ----    通用设置    ----    */

        updateInfo()
    }

    override fun back(): Boolean = true

    private fun updateInfo() {
        val user = Config.user
        if (user != null) {
            v.name.text = user.name
            v.avatar.load(rilNet, user.avatarPath, Config.cache_key_avatar)
            v.signature.text = user.signature
            v.inviter.text = user.inviterName ?: ""
            v.wall.load(rilNet, user.wallPath, Config.cache_key_wall)
        }
        else {
            v.name.text = pages.getResString(R.string.default_name)
            v.avatar.pureColor = pages.getResColor(R.color.micro_gray)
            v.signature.text = pages.getResString(R.string.default_signature)
            v.inviter.text = ""
            v.wall.pureColor = pages.getResColor(R.color.micro_gray)
        }
        v.weiboList.setTags(Config.weibo_users.names)
    }

    @NewThread
    private fun logoff() {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context, "退出登录中...")
            val result = withContext(Dispatchers.IO) { API.UserAPI.logoff(Config.token) }
            loading.dismiss()
            when (result.code) {
                API.Code.SUCCESS -> {
                    Config.token = ""
                    Config.user = null
                    pages.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, null)
                    pages.pop()
                }
                API.Code.UNAUTHORIZED -> {
                    tip(Tip.WARNING, result.msg)
                    Config.token = ""
                    Config.user = null
                    pages.pop()
                    pages.navigate(FragmentLogin(pages))
                }
                else -> tip(Tip.ERROR, result.msg)
            }
        }
    }

    @NewThread
    private fun addWeiboUser(uid: String) {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val result = withContext(Dispatchers.IO) { WeiboAPI.extractContainerId(uid) }
            loading.dismiss()
            if (result != null) {
                val weiboUsers = Config.weibo_users
                val name = result[1]
                weiboUsers[result[0]] = WeiboUserStorage(name, result[2])
                Config.weibo_users = weiboUsers
                v.weiboList.addTag(name)
            }
            else tip(Tip.ERROR, "解析微博用户失败")
        }
    }

    @NewThread
    private fun updateName(name: String) {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val result = withContext(Dispatchers.IO) { API.UserAPI.updateName(Config.token, name) }
            loading.dismiss()
            if (result.success) {
                tip(Tip.SUCCESS, result.msg)
                v.name.text = name
                pages.sendMessage(RachelTab.me, RachelMessage.ME_REQUEST_USER_INFO)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    @NewThread
    private fun updateAvatar(filename: String) {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val result = withContext(Dispatchers.IO) { API.UserAPI.updateAvatar(Config.token, filename) }
            loading.dismiss()
            if (result.success) {
                tip(Tip.SUCCESS, result.msg)
                val user = Config.user!!
                Config.cache_key_avatar_meta.update()
                v.avatar.load(rilNet, user.avatarPath, Config.cache_key_avatar)
                pages.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, user)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }
}