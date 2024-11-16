package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.haibin.calendarview.Calendar
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.activity.ShowActivity
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentMeBinding
import com.yinlin.rachel.date
import com.yinlin.rachel.dialog.BottomDialogUserCard
import com.yinlin.rachel.gotoTaobaoShop
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.pureColor
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import com.yinlin.rachel.view.ActivityCalendarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentMe(pages: RachelPages) : RachelFragment<FragmentMeBinding>(pages)  {
    private val rilNet = RachelImageLoader(pages.context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

    private val bottomDialogUserCard = BottomDialogUserCard(this)

    override fun bindingClass() = FragmentMeBinding::class.java

    override fun init() {
        // 扫码
        v.buttonScan.rachelClick { pages.navigate(FragmentScanQRCode(pages)) }

        // 名片
        v.buttonProfile.rachelClick {
            val user = Config.loginUser
            if (user != null) bottomDialogUserCard.update(user).show()
            else {
                tip(Tip.WARNING, "请先登录")
                pages.navigate(FragmentLogin(pages))
            }
        }

        // 设置
        v.buttonSettings.rachelClick { pages.navigate(FragmentSettings(pages)) }

        // 店铺
        v.buttonShop.rachelClick { gotoTaobaoShop(pages.context, "280201975") }

        // 签到
        v.buttonSignIn.rachelClick {
            if (Config.isLogin) signin()
            else {
                tip(Tip.WARNING, "请先登录")
                pages.navigate(FragmentLogin(pages))
            }
        }

        // 好友
        v.buttonFriend.rachelClick {
            tip(Tip.INFO, "即将开放, 敬请期待新版本!")
        }

        // 主题
        v.buttonTopic.rachelClick {
            val user = Config.loginUser
            if (user != null) pages.navigate(FragmentProfile(pages, user.uid))
            else {
                tip(Tip.WARNING, "请先登录")
                pages.navigate(FragmentLogin(pages))
            }
        }

        // 邮箱
        v.buttonMail.rachelClick {
            if (Config.isLogin) pages.navigate(FragmentMail(pages))
            else {
                tip(Tip.WARNING, "请先登录")
                pages.navigate(FragmentLogin(pages))
            }
        }

        // 徽章
        v.buttonMedal.rachelClick {
            tip(Tip.INFO, "即将开放, 敬请期待新版本!")
        }

        // 下拉刷新
        v.container.setOnRefreshListener {
            if (Config.isLogin) { requestUserInfo() }
            else pages.navigate(FragmentLogin(pages))
            if (v.container.isRefreshing) v.container.finishRefresh()
        }

        // 日历
        v.calendarMonth.text = "${v.calendar.curYear}年${v.calendar.curMonth}月"
        v.calendar.listener = object: ActivityCalendarView.Listener {
            override fun onClick(calendar: Calendar) {
                if (calendar.hasScheme()) getActivityInfo(calendar)
            }
            override fun onLongClick(calendar: Calendar) {
                val user = Config.loginUser
                if (user != null && user.hasPrivilegeVIPCalendar) {
                    if (calendar.hasScheme()) RachelDialog.confirm(pages.context, content="确定要删除该活动吗?") {
                        deleteActivity(calendar)
                    }
                    else pages.navigate(FragmentAddActivity(pages, calendar))
                }
            }
            override fun onMonthChanged(year: Int, month: Int) {
                v.calendarMonth.text = "${year}年${month}月"
            }
        }
        v.buttonActivityRefresh.rachelClick { getActivities(true) }

        postDelay(500) { getActivities(false) }
    }

    override fun update() {
        updateUserInfo(Config.user)
    }

    override fun quit() {
        bottomDialogUserCard.release()
    }

    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            RachelMessage.ME_UPDATE_USER_INFO -> updateUserInfo(args[0] as User?)
            RachelMessage.ME_REQUEST_USER_INFO -> if (Config.isLogin) requestUserInfo()
            RachelMessage.ME_ADD_ACTIVITY -> addActivity(args[0] as Calendar, args[1] as ShowActivity)
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
                    val user = result.data
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

    @NewThread
    private fun getActivities(showLoading: Boolean) {
        lifecycleScope.launch {
            val loading = if (showLoading) RachelDialog.loading(pages.context) else null
            val result = withContext(Dispatchers.IO) { API.UserAPI.getActivities() }
            loading?.dismiss()
            if (result.success) {
                v.calendar.setActivities(result.data)
                v.calendar.scrollToCurrent(true)
            }
        }
    }

    // 获取活动详情
    private fun getActivityInfo(calendar: Calendar) {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val result = withContext(Dispatchers.IO) { API.UserAPI.getActivityInfo(calendar.date) }
            loading.dismiss()
            if (result.success) {
                pages.navigate(FragmentShowActivity(pages, result.data))
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    // 添加活动
    @NewThread
    private fun addActivity(calendar: Calendar, show: ShowActivity) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { API.UserAPI.addActivity(Config.token, show) }
            if (result.success) {
                tip(Tip.SUCCESS, result.msg)
                v.calendar.addActivity(calendar, show.title)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    // 删除活动
    @NewThread
    private fun deleteActivity(calendar: Calendar) {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val result = withContext(Dispatchers.IO) { API.UserAPI.deleteActivity(Config.token, calendar.date) }
            loading.dismiss()
            if (result.success) {
                v.calendar.removeSchemeDate(calendar)
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    // 签到
    @NewThread
    private fun signin() {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context, "签到中...")
            val result = withContext(Dispatchers.IO) { API.UserAPI.signin(Config.token) }
            loading.dismiss()
            if (result.success) {
                tip(Tip.SUCCESS, result.msg)
                requestUserInfo()
            }
            else tip(Tip.ERROR, result.msg)
        }
    }
}