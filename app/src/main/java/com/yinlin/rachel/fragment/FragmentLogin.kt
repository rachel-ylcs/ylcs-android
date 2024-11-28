package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentLoginBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.rachelClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentLogin(main: MainActivity) : RachelFragment<FragmentLoginBinding>(main) {
    override fun bindingClass() = FragmentLoginBinding::class.java

    override fun init() {
        // 没有账号
        v.labelRegister.rachelClick { showRegister() }
        // 忘记密码
        v.labelNopwd.rachelClick { showForgotPassword() }
        // 登录
        v.buttonLogin.rachelClick { login() }
        // 返回登录
        v.registerLabelBackLogin.rachelClick { showLogin() }
        v.forgotPasswordLabelBackLogin.rachelClick { showLogin() }
        // 注册
        v.buttonRegister.rachelClick { register() }
        // 忘记密码申请
        v.buttonForgotPassword.rachelClick { forgotPassword() }
    }

    override fun back(): Boolean = true

    private fun showLogin() {
        v.loginName.text = ""
        v.loginPwd.text = ""
        v.registerContainer.collapse(false)
        v.forgotPasswordContainer.collapse(false)
        v.loginContainer.isExpand = true
    }

    private fun showRegister() {
        v.registerName.text = ""
        v.registerPwd.text = ""
        v.registerConfirmPwd.text = ""
        v.registerInviter.text = ""
        v.loginContainer.collapse(false)
        v.forgotPasswordContainer.collapse(false)
        v.registerContainer.isExpand = true
    }

    private fun showForgotPassword() {
        v.forgotPasswordName.text = ""
        v.forgotPasswordPwd.text = ""
        v.loginContainer.collapse(false)
        v.registerContainer.collapse(false)
        v.forgotPasswordContainer.isExpand = true
    }

    @NewThread
    fun login() {
        val name = v.loginName.text
        val pwd = v.loginPwd.text
        if (User.Companion.Constraint.name(name) && User.Companion.Constraint.password(pwd)) {
            lifecycleScope.launch {
                val loading = main.loading
                val result1 = withContext(Dispatchers.IO) { API.UserAPI.login(name, pwd) }
                if (result1.success) {
                    val token = result1.data.token
                    Config.token = token
                    val result2 = withContext(Dispatchers.IO) { API.UserAPI.getInfo(token) }
                    if (result2.success) {
                        val user = result2.data
                        Config.user = user
                        main.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, user)
                    }
                    main.pop()
                }
                else tip(Tip.ERROR, result1.msg)
                loading.dismiss()
            }
        }
        else tip(Tip.WARNING, "ID和密码不合规则")
    }

    @NewThread
    fun register() {
        val name = v.registerName.text
        val pwd = v.registerPwd.text
        val confirmPwd = v.registerConfirmPwd.text
        val inviterName = v.registerInviter.text
        if (User.Companion.Constraint.name(name, inviterName) && User.Companion.Constraint.password(pwd, confirmPwd)) {
            if (pwd == confirmPwd) {
                lifecycleScope.launch {
                    val loading = main.loading
                    val result = withContext(Dispatchers.IO) { API.UserAPI.register(name, pwd, inviterName) }
                    loading.dismiss()
                    if (result.success) {
                        tip(Tip.SUCCESS, result.msg)
                        showLogin()
                    }
                    else tip(Tip.ERROR, result.msg)
                }
            }
            else tip(Tip.WARNING, "两次输入的密码不相同")
        }
        else tip(Tip.WARNING, "ID、密码、邀请人长度不合规则")
    }

    @NewThread
    fun forgotPassword() {
        val name = v.forgotPasswordName.text
        val pwd = v.forgotPasswordPwd.text
        if (User.Companion.Constraint.name(name) && User.Companion.Constraint.password(pwd)) {
            lifecycleScope.launch {
                val loading = main.loading
                val result = withContext(Dispatchers.IO) { API.UserAPI.forgotPassword(name, pwd) }
                loading.dismiss()
                if (result.success) {
                    tip(Tip.SUCCESS, result.msg)
                    showLogin()
                }
                else tip(Tip.ERROR, result.msg)
            }
        }
        else tip(Tip.WARNING, "ID和新密码不合规则")
    }
}