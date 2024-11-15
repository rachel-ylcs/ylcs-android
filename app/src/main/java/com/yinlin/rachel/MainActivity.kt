package com.yinlin.rachel

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.ActivityMainBinding
import com.yinlin.rachel.fragment.FragmentLogin
import com.yinlin.rachel.model.RachelActivity
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : RachelActivity() {
    private lateinit var v: ActivityMainBinding
    private lateinit var pages: RachelPages

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        enableEdgeToEdge()

        v = ActivityMainBinding.inflate(layoutInflater)
        val view = v.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pages = RachelPages(this, v.btv, arrayOf(
            RachelTab.msg, RachelTab.discovery, RachelTab.music, RachelTab.world, RachelTab.me
        ), RachelTab.msg, R.id.frame)

        pages.handler.post { pages.processIntent(intent) }
        pages.handler.postDelayed({
            lifecycleScope.launch {
                val token = Config.token
                if (token.isNotEmpty()) {
                    val result1 = withContext(Dispatchers.IO) { API.UserAPI.updateToken(token) }
                    when (result1.code) {
                        API.Code.SUCCESS -> {
                            Config.token = result1["token"].asString
                            val result2 = withContext(Dispatchers.IO) { API.UserAPI.getInfo(token) }
                            if (result2.success) {
                                val user: User = result2.fetch()
                                Config.user = user
                                pages.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, user)
                            }
                        }
                        API.Code.UNAUTHORIZED -> {
                            tip(Tip.WARNING, result1.msg)
                            Config.token = ""
                            Config.user = null
                            pages.sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, null)
                            pages.navigate(FragmentLogin(pages))
                        }
                        else -> tip(Tip.ERROR, result1.msg)
                    }
                }
            }
        }, 500)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.apply { pages.processIntent(this) }
    }

    @SuppressLint("MissingSuperCall") @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (pages.goBack()) pages.pop()
    }
}