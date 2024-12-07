package com.yinlin.rachel.fragment

import com.haibin.calendarview.Calendar
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.activity.ShowActivity
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentMeBinding
import com.yinlin.rachel.tool.date
import com.yinlin.rachel.model.RachelAppIntent
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.tool.pureColor
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.sheet.SheetUserCard
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.view.ActivityCalendarView

class FragmentMe(main: MainActivity) : RachelFragment<FragmentMeBinding>(main)  {
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
            if (user != null) SheetUserCard(this, user).show()
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

    override fun back() = BackState.HOME

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

    @IOThread
    private fun requestUserInfo() {
        startIOWithResult({ API.UserAPI.getInfo(Config.token) }) {
            when (it.code) {
                API.Code.SUCCESS -> {
                    val user = it.data
                    Config.user = user
                    updateUserInfo(user)
                }
                API.Code.UNAUTHORIZED -> {
                    tip(Tip.WARNING, it.msg)
                    Config.token = ""
                    Config.user = null
                    updateUserInfo(null)
                    main.navigate(FragmentLogin(main))
                }
                else -> tip(Tip.ERROR, it.msg)
            }
        }
    }

    @IOThread
    private fun getActivities(showLoading: Boolean) {
        if (showLoading) v.calendarMonth.loading = true
        startIOWithResult({ API.UserAPI.getActivities() }) {
            if (showLoading) v.calendarMonth.loading = false
            if (it.success) {
                v.calendar.setActivities(it.data)
                v.calendar.scrollToCurrent(true)
            }
        }
    }

    // 获取活动详情
    @IOThread
    private fun getActivityInfo(calendar: Calendar) {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.getActivityInfo(calendar.date) }) {
            loading.dismiss()
            if (it.success) main.navigate(FragmentShowActivity(main, it.data))
            else tip(Tip.ERROR, it.msg)
        }
    }

    // 添加活动
    @IOThread
    private fun addActivity(calendar: Calendar, show: ShowActivity) {
        startIOWithResult({ API.UserAPI.addActivity(Config.token, show) }) {
            if (it.success) {
                tip(Tip.SUCCESS, it.msg)
                v.calendar.addActivity(calendar, show.title)
            }
            else tip(Tip.ERROR, it.msg)
        }
    }

    // 删除活动
    @IOThread
    private fun deleteActivity(calendar: Calendar) {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.deleteActivity(Config.token, calendar.date) }) {
            loading.dismiss()
            if (it.success) {
                v.calendar.removeSchemeDate(calendar)
                tip(Tip.SUCCESS, it.msg)
            }
            else tip(Tip.ERROR, it.msg)
        }
    }

    // 签到
    @IOThread
    private fun signin() {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.signin(Config.token) }) {
            loading.dismiss()
            if (it.success) {
                tip(Tip.SUCCESS, it.msg)
                requestUserInfo()
            }
            else tip(Tip.ERROR, it.msg)
        }
    }
}