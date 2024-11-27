package com.yinlin.rachel

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.databinding.ActivityMainBinding
import com.yinlin.rachel.fragment.FragmentImportMod
import com.yinlin.rachel.fragment.FragmentImportNetEaseCloud
import com.yinlin.rachel.fragment.FragmentLogin
import com.yinlin.rachel.fragment.FragmentProfile
import com.yinlin.rachel.model.RachelActivity
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.KClass

class MainActivity : RachelActivity() {
    private lateinit var v: ActivityMainBinding
    private val manager: FragmentManager = supportFragmentManager
    private lateinit var fragments: List<RachelFragment<*>>
    private val fragmentStack = mutableListOf<RachelFragment<*>>()
    private lateinit var currentFragment: RachelFragment<*>
    private var currentMainIndex: Int = 0

    lateinit var handler: Handler

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
        createPages()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initPages()
    }

    private fun initPages() {
        // 3. 更新 Token
        lifecycleScope.launch {
            val loadingDialog = loading

            // 1. 处理 Intent
            processIntent(intent)

            // 2. 注册 MusicService
            val sessionToken = SessionToken(this@MainActivity, ComponentName(this@MainActivity, MusicService::class.java))
            val mediaControllerFuture = MediaController.Builder(this@MainActivity, sessionToken).buildAsync()
            val player = withContext(Dispatchers.IO) { mediaControllerFuture.get() }
            sendMessage(RachelTab.music, RachelMessage.PREPARE_PLAYER, player)

            // 3. 更新 Token
            val token = Config.token
            if (token.isNotEmpty()) {
                if (Config.token_daily != currentDateInteger) {
                    val result = withContext(Dispatchers.IO) {
                        withTimeoutOrNull(5000) { API.UserAPI.updateToken(token) } ?: API.errResult()
                    }
                    when (result.code) {
                        API.Code.SUCCESS -> Config.token = result.data.token
                        API.Code.UNAUTHORIZED -> {
                            tip(Tip.WARNING, result.msg)
                            Config.token = ""
                            Config.user = null
                            sendMessage(RachelTab.me, RachelMessage.ME_UPDATE_USER_INFO, null)
                            navigate(FragmentLogin(this@MainActivity))
                        }
                    }
                }
            }
            
            loadingDialog.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        currentFragment.onActivityStart()
    }

    override fun onStop() {
        super.onStop()
        currentFragment.onActivityStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    @SuppressLint("MissingSuperCall") @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentFragment.back()) pop()
    }

    private val isMain: Boolean get() = fragmentStack.isEmpty()
    private val isNotMain: Boolean get() = fragmentStack.isNotEmpty()

    fun updateTabStatus() {
        if (isNotMain && v.btv.visible) v.btv.visible = false
    }

    private fun createPages() {
        handler = Handler(mainLooper)

        // 初始化页面
        val transaction = manager.beginTransaction()

        val tabs = arrayOf(RachelTab.world, RachelTab.msg, RachelTab.music, RachelTab.discovery, RachelTab.me)
        tabs.sortWith { a, b -> a.index - b.index }
        fragments = tabs.map { it.prototype.getConstructor(MainActivity::class.java).newInstance(this) }
        for (fragment in fragments) transaction.add(R.id.frame, fragment).hide(fragment)
        val home = RachelTab.world.index
        currentFragment = fragments[home]
        currentMainIndex = home
        transaction.show(currentFragment)
        transaction.commit()

        // 初始化底部导航栏
        v.btv.apply {
            setItems(tabs, home)
            listener = {
                if (isMain) {
                    val oldFragment = currentFragment
                    currentFragment = fragments[it.index]
                    currentMainIndex = it.index
                    manager.beginTransaction().hide(oldFragment).show(currentFragment).commit()
                }
            }
        }
    }

    fun navigate(des: RachelFragment<*>) {
        fragmentStack += des
        val transaction = manager.beginTransaction().hide(currentFragment).add(R.id.frame, des).addToBackStack(null)
        currentFragment = des
        transaction.commit()
    }

    fun pop() {
        if (isNotMain) {
            fragmentStack.removeLastOrNull()
            manager.popBackStackImmediate()
            if (isMain) {
                v.btv.visible = true
                currentFragment = fragments[currentMainIndex]
            }
            else currentFragment = fragmentStack.last()
        }
    }

    fun <T : RachelFragment<*>> popMessage(cls: KClass<T>, msg: RachelMessage, vararg args: Any?) {
        val fragmentCount = fragmentStack.size
        if (fragmentCount > 0) {
            if (fragmentCount > 1) {
                val fragment = fragmentStack[fragmentCount - 2]
                if (cls.isInstance(fragment)) {
                    if (fragment.isAttached) fragment.message(msg, *args)
                }
            }
            pop()
        }
    }

    fun sendMessage(item: RachelTab, msg: RachelMessage, vararg args: Any?) {
        val fragment = fragments[item.index]
        if (fragment.isAttached) fragment.message(msg, *args)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> sendMessageForResult(item: RachelTab, msg: RachelMessage, vararg args: Any?): T? {
        val fragment = fragments[item.index]
        return if (fragment.isAttached) fragment.messageForResult(msg, *args) as T? else null
    }

    fun <T : RachelFragment<*>> sendBottomMessage(cls: KClass<T>, msg: RachelMessage, vararg args: Any?) {
        val fragmentCount = fragmentStack.size
        if (fragmentCount > 1) {
            val fragment = fragmentStack[fragmentCount - 2]
            if (cls.isInstance(fragment) && fragment.isAttached) fragment.message(msg, *args)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : RachelFragment<*>, R> sendBottomMessageForResult(cls: KClass<T>, msg: RachelMessage, vararg args: Any?): R? {
        val fragmentCount = fragmentStack.size
        if (fragmentCount > 1) {
            val fragment = fragmentStack[fragmentCount - 2]
            if (cls.isInstance(fragment)) {
                if (fragment.isAttached) return fragment.messageForResult(msg, *args) as R?
            }
        }
        return null
    }

    private fun processSchemeContent(uri: Uri) {
        navigate(FragmentImportMod(this, uri))
    }

    private fun processSchemeRachel(uri: Uri, args: HashMap<String, String>) {
        when (uri.path) {
            "/openProfile" -> {
                args["uid"]?.toIntOrNull()?.let { navigate(FragmentProfile(this, it)) }
            }
        }
    }

    private fun processActionMain() { }

    private fun processActionView(uri: Uri) {
        val args = HashMap<String, String>()
        for (name in uri.queryParameterNames) uri.getQueryParameter(name)?.let { args[name] = it }
        when (uri.scheme) {
            "content" -> processSchemeContent(uri)
            "rachel" -> processSchemeRachel(uri, args)
        }
    }

    private fun processSendText(title: String, text: String) {
        when (title) {
            "网易云音乐" -> navigate(FragmentImportNetEaseCloud(this@MainActivity, text, true))
        }
    }

    private fun processActionSend(type: String, bundle: Bundle) {
        when (type) {
            "text/plain" -> {
                val title = bundle.getString(Intent.EXTRA_TITLE)
                val text = bundle.getString(Intent.EXTRA_TEXT)
                if (title != null && text != null) processSendText(title, text)
            }
        }
    }

    fun processUri(uri: Uri) {
        val args = HashMap<String, String>()
        for (name in uri.queryParameterNames) uri.getQueryParameter(name)?.let { args[name] = it }
        processSchemeRachel(uri, args)
    }

    private fun processIntent(intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_MAIN -> processActionMain()
                Intent.ACTION_VIEW -> { intent.data?.let { processActionView(it) } }
                Intent.ACTION_SEND -> {
                    val bundle = intent.extras
                    val type = intent.type
                    if (bundle != null && type != null) processActionSend(type, bundle)
                }
            }
        }
        catch (_: Exception) { }
    }

    fun isForeground(tab: RachelTab): Boolean = isMain && currentMainIndex == tab.index

    val loading: RachelDialog.Companion.DialogLoading get() = RachelDialog.loading(this, handler)

    // 资源
    fun rs(@StringRes id: Int) = getString(id)
    fun rc(@ColorRes id: Int) = getColor(id)
    fun rd(@DimenRes id: Int) = resources.getDimensionPixelSize(id)

    // 取当前版本
    val appVersion: Long get() = packageManager.getPackageInfo(packageName, 0).longVersionCode
    fun appVersionName(version: Long): String = "${version / 100}.${version / 10 % 10}.${version % 10}"
}