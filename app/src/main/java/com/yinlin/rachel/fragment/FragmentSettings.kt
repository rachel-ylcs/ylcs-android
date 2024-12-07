package com.yinlin.rachel.fragment

import com.bumptech.glide.Glide
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentSettingsBinding
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelPictureSelector
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.pureColor
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.sheet.SheetCrashLog
import com.yinlin.rachel.tool.deleteSafely
import com.yinlin.rachel.tool.folderSizeString
import com.yinlin.rachel.tool.pathCache
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.startIOWithResult

class FragmentSettings(main: MainActivity) : RachelFragment<FragmentSettingsBinding>(main) {
    override fun bindingClass() = FragmentSettingsBinding::class.java

    override fun init() {
        /*    ----    账号设置    ----    */

        // 更换头像
        v.avatar.rachelClick {
            if (Config.isLogin) RachelPictureSelector.single(main, 256, 256, true) { updateAvatar(it) }
            else tip(Tip.WARNING, "请先登录")
        }

        // 更换昵称
        v.name.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.coin < User.RENAME_COIN_COST) tip(Tip.WARNING, "你的银币不够哦~")
                else RachelDialog.input(main, "请输入新ID(改名卡: 5银币)", User.Companion.Constraint.MAX_USER_NAME_LENGTH) {
                    if (User.Companion.Constraint.name(it)) updateName(it)
                    else tip(Tip.WARNING, "ID不合规则")
                }
            }
            else tip(Tip.WARNING, "请先登录")
        }

        // 更新个性签名
        v.signature.rachelClick {
            if (Config.isLogin) RachelDialog.input(main, "请输入个性签名", 64) { updateSignature(it) }
            else tip(Tip.WARNING, "请先登录")
        }

        // 更新背景墙
        v.wall.rachelClick {
            if (Config.isLogin) RachelPictureSelector.single(main, 910, 512, false) { updateWall(it) }
            else tip(Tip.WARNING, "请先登录")
        }

        // 退出登录
        v.logoff.rachelClick {
            if (Config.token.isNotEmpty()) { RachelDialog.confirm(main, content="是否退出登录") { logoff() } }
            else tip(Tip.WARNING, "请先登录")
        }

        updateInfo()

        /*    ----    个性化设置    ----    */

        /*    ----    资讯设置    ----    */
        v.weibo.rachelClick {
            main.navigate(FragmentWeiboUserList(main))
        }

        /*    ----    听歌设置    ----    */

        // 音频焦点
        v.switchMusicFocus.isChecked = Config.music_focus
        v.switchMusicFocus.setOnCheckedChangeListener { _, isChecked -> Config.music_focus = isChecked }

        // 状态栏歌词
        v.lyricsSettings.rachelClick { main.navigate(FragmentLyricsSettings(main)) }

        // 歌单云备份
        v.buttonUploadPlaylist.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeBackup) RachelDialog.confirm(main, content="是否将本地所有歌单覆盖云端") { uploadPlaylist() }
                else tip(Tip.WARNING, "你没有权限")
            }
            else tip(Tip.WARNING, "请先登录")
        }

        // 歌单云还原
        v.buttonDownloadPlaylist.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeBackup) RachelDialog.confirm(main, content="是否从云端覆盖所有本地歌单") { downloadPlaylist() }
                else tip(Tip.WARNING, "你没有权限")
            }
            else tip(Tip.WARNING, "请先登录")
        }

        /*    ----    通用设置    ----    */

        v.clearCache.rachelClick { clearCacheSize() }
        updateCacheSize()

        v.crashLog.rachelClick { SheetCrashLog(this).show() }

        v.version.text = main.appVersionName(main.appVersion)
        v.checkUpdate.rachelClick { main.navigate(FragmentUpdate(main)) }

        v.feedback.rachelClick {
            if (Config.isLogin) RachelDialog.input(main, "悉听良计, 赠以银币!", 256, 10) { sendFeedback(it) }
            else tip(Tip.WARNING, "请先登录")
        }

        v.privacyPolicy.rachelClick { main.navigate(FragmentPrivacyPolicy(main)) }

        v.about.rachelClick { main.navigate(FragmentAbout(main)) }
    }

    override fun back() = BackState.POP

    private fun updateInfo() {
        val user = Config.user
        if (user != null) {
            v.name.text = user.name
            v.avatar.load(user.avatarPath, Config.cache_key_avatar)
            v.signature.text = user.signature
            v.inviter.text = user.inviterName ?: ""
            v.wall.load(user.wallPath, Config.cache_key_wall)
        }
        else {
            v.name.text = main.rs(R.string.default_name)
            v.avatar.pureColor = main.rc(R.color.micro_gray)
            v.signature.text = main.rs(R.string.default_signature)
            v.inviter.text = ""
            v.wall.pureColor = main.rc(R.color.micro_gray)
        }
    }

    @IOThread
    private fun logoff() {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.logoff(Config.token) }) {
            loading.dismiss()
            when (it.code) {
                API.Code.SUCCESS -> {
                    Config.token = ""
                    Config.user = null
                    main.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, null)
                    main.pop()
                }
                API.Code.UNAUTHORIZED -> {
                    tip(Tip.WARNING, it.msg)
                    Config.token = ""
                    Config.user = null
                    main.pop()
                    main.navigate(FragmentLogin(main))
                }
                else -> tip(Tip.ERROR, it.msg)
            }
        }
    }

    @IOThread
    private fun updateName(name: String) {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.updateName(Config.token, name) }) {
            loading.dismiss()
            if (it.success) {
                tip(Tip.SUCCESS, it.msg)
                v.name.text = name
                main.sendMessage(RachelTab.me, RachelMessage.ME_REQUEST_USER_INFO)
            }
            else tip(Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun updateAvatar(filename: String) {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.updateAvatar(Config.token, filename) }) {
            loading.dismiss()
            if (it.success) {
                tip(Tip.SUCCESS, it.msg)
                val user = Config.user!!
                Config.cache_key_avatar_meta.update()
                v.avatar.load(user.avatarPath, Config.cache_key_avatar)
                main.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, user)
            }
            else tip(Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun updateSignature(signature: String) {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.updateSignature(Config.token, signature) }) {
            loading.dismiss()
            if (it.success) {
                tip(Tip.SUCCESS, it.msg)
                v.signature.text = signature
                main.sendMessage(RachelTab.me, RachelMessage.ME_REQUEST_USER_INFO)
            }
            else tip(Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun updateWall(wall: String) {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.updateWall(Config.token, wall) }) {
            loading.dismiss()
            if (it.success) {
                tip(Tip.SUCCESS, it.msg)
                val user = Config.user!!
                Config.cache_key_wall_meta.update()
                v.wall.load(user.wallPath, Config.cache_key_wall)
                main.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, user)
            }
            else tip(Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun uploadPlaylist() {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.uploadPlaylist(Config.token, Config.playlist) }) {
            loading.dismiss()
            tip(if (it.success) Tip.SUCCESS else Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun downloadPlaylist() {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.downloadPlaylist(Config.token) }) {
            loading.dismiss()
            if (it.success) {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_RELOAD_PLAYLIST, it.data)
                tip(Tip.SUCCESS, it.msg)
            }
            else tip(Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun sendFeedback(content: String) {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.sendFeedback(Config.token, content) }) {
            loading.dismiss()
            tip(if (it.success) Tip.SUCCESS else Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun updateCacheSize() {
        startIOWithResult({ pathCache.folderSizeString }) {
            v.cacheSize.text = it
        }
    }

    @IOThread
    private fun clearCacheSize() {
        v.clearCache.isEnabled = false
        v.cacheSize.text = "清理缓存中..."
        startIOWithResult({
            pathCache.deleteSafely()
            pathCache.folderSizeString
        }) {
            v.clearCache.isEnabled = true
            v.cacheSize.text = it
        }
    }
}