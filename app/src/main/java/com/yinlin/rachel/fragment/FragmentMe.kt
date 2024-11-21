package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.haibin.calendarview.Calendar
import com.yinlin.rachel.Config
import com.yinlin.rachel.MainActivity
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
import com.yinlin.rachel.model.RachelAppIntent
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.pureColor
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.view.ActivityCalendarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentMe(main: MainActivity) : RachelFragment<FragmentMeBinding>(main)  {
    private val bottomDialogUserCard = BottomDialogUserCard(this)

    override fun bindingClass() = FragmentMeBinding::class.java

    override fun init() {
        v.avatar.rachelClick {
            if (!Config.isLogin) {
                tip(Tip.WARNING, "请先登录")
                main.navigate(FragmentLogin(main))
            }
        }

        // 扫码
        v.buttonScan.rachelClick { main.navigate(FragmentScanQRCode(main)) }

        // 名片
        v.buttonProfile.rachelClick {
            val user = Config.loginUser
            if (user != null) bottomDialogUserCard.update(user).show()
            else {
                tip(Tip.WARNING, "请先登录")
                main.navigate(FragmentLogin(main))
            }
        }

        // 设置
        v.buttonSettings.rachelClick { main.navigate(FragmentSettings(main)) }

        // 店铺
        v.buttonShop.rachelClick {
            val intent = RachelAppIntent.Taobao("280201975")
            if (!intent.start(main)) tip(Tip.WARNING, "未安装\"${intent.name}\"")
        }

        // 签到
        v.buttonSignIn.rachelClick {
            if (Config.isLogin) signin()
            else {
                tip(Tip.WARNING, "请先登录")
                main.navigate(FragmentLogin(main))
            }
        }

        // 好友
        v.buttonFriend.rachelClick {
            tip(Tip.INFO, "即将开放, 敬请期待新版本!")
        }

        // 主题
        v.buttonTopic.rachelClick {
            val user = Config.loginUser
            if (user != null) main.navigate(FragmentProfile(main, user.uid))
            else {
                tip(Tip.WARNING, "请先登录")
                main.navigate(FragmentLogin(main))
            }
        }

        // 邮箱
        v.buttonMail.rachelClick {
            if (Config.isLogin) main.navigate(FragmentMail(main))
            else {
                tip(Tip.WARNING, "请先登录")
                main.navigate(FragmentLogin(main))
            }
        }

        // 徽章
        v.buttonMedal.rachelClick {
            tip(Tip.INFO, "即将开放, 敬请期待新版本!")
        }

        // 下拉刷新
        v.container.setOnRefreshListener {
            if (Config.isLogin) { requestUserInfo() }
            else main.navigate(FragmentLogin(main))
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
                    if (calendar.hasScheme()) RachelDialog.confirm(main, content="确定要删除该活动吗?") {
                        deleteActivity(calendar)
                    }
                    else main.navigate(FragmentAddActivity(main, calendar))
                }
            }
            override fun onMonthChanged(year: Int, month: Int) {
                v.calendarMonth.text = "${year}年${month}月"
            }
        }
        v.buttonActivityRefresh.rachelClick { getActivities(true) }
    }

    override fun start() {
        updateUserInfo(Config.user)
        getActivities(false)
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
            v.avatar.load(user.avatarPath, Config.cache_key_avatar)
            v.wall.load(user.wallPath, Config.cache_key_wall)
        }
        else {
            v.name.text = main.rs(R.string.default_name)
            v.label.setDefaultLabel()
            v.signature.text = main.rs(R.string.default_signature)
            v.level.text = "1"
            v.coin.text = "0"
            v.avatar.pureColor = main.rc(R.color.white)
            v.wall.pureColor = main.rc(R.color.dark)
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
                    main.navigate(FragmentLogin(main))
                }
                else -> tip(Tip.ERROR, result.msg)
            }
        }
    }

    @NewThread
    private fun getActivities(showLoading: Boolean) {
        lifecycleScope.launch {
            val loading = if (showLoading) main.loading else null
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
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.getActivityInfo(calendar.date) }
            loading.dismiss()
            if (result.success) {
                main.navigate(FragmentShowActivity(main, result.data))
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
            val loading = main.loading
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
            val loading = main.loading
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