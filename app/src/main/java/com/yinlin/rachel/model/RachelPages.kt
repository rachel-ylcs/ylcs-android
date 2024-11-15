package com.yinlin.rachel.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.view.BottomTabView
import com.yinlin.rachel.visible
import kotlin.reflect.KClass

class RachelPages(
    val activity: FragmentActivity,
    private val btv: BottomTabView,
    private val items: Array<RachelTab>,
    home: RachelTab,
    @IdRes val mainFrame: Int
) {
    val context: Context = activity
    private val manager: FragmentManager = activity.supportFragmentManager
    private val fragments = arrayOfNulls<RachelFragment<*>>(items.size)
    private val fragmentStack = ArrayList<RachelFragment<*>>()
    private val isMain get() = fragmentStack.isEmpty()
    private val isNotMain get() = fragmentStack.isNotEmpty()
    private var currentFragment: RachelFragment<*>
    private var currentMainFragment: RachelFragment<*>
    val handler: Handler = Handler(activity.mainLooper)
    val ril: RachelImageLoader = RachelImageLoader(activity)

    init {
        // 首页
        val homeFragment = items[home.index].prototype.getConstructor(RachelPages::class.java)
            .newInstance(this) as RachelFragment<*>
        fragments[home.index] = homeFragment
        currentFragment = homeFragment
        currentMainFragment = homeFragment
        manager.beginTransaction().add(mainFrame, homeFragment).commit()

        // 初始化底部导航栏
        btv.apply {
            setItems(items, home.index)
            listener = {
                if (isMain) {
                    val transaction = manager.beginTransaction().hide(currentFragment)
                    val index = it.index
                    var nextFragment = fragments[index]
                    if (nextFragment == null) {
                        nextFragment = it.prototype.getConstructor(RachelPages::class.java)
                            .newInstance(this@RachelPages) as RachelFragment<*>
                        fragments[index] = nextFragment
                        transaction.add(mainFrame, nextFragment)
                    }
                    else transaction.show(nextFragment)
                    currentFragment = nextFragment
                    currentMainFragment = nextFragment
                    transaction.commit()
                }
            }
        }
    }

    fun checkBottomLayoutStatus() {
        if (isNotMain && btv.visible) btv.visible = false
    }

    fun isForeground(item: RachelTab) = isMain && currentFragment == fragments[item.index]

    fun isForeground(fragment: RachelFragment<*>) = isMain && currentFragment == fragment

    fun navigate(des: RachelFragment<*>) {
        fragmentStack += des
        val transaction = manager.beginTransaction().hide(currentFragment).add(mainFrame, des).addToBackStack(null)
        currentFragment = des
        transaction.commit()
    }

    fun pop() {
        if (isNotMain) {
            fragmentStack.removeLast()
            manager.popBackStackImmediate()
            if (isMain) {
                btv.visible = true
                currentFragment = currentMainFragment
            }
            else currentFragment = fragmentStack.last()
        }
    }

    fun <T : RachelFragment<*>> popMessage(cls: KClass<T>, msg: RachelMessage, vararg args: Any?) {
        val fragmentCount = fragmentStack.size
        if (fragmentCount > 0) {
            if (fragmentCount > 1) {
                val fragment = fragmentStack[fragmentCount - 2]
                if (cls.isInstance(fragment)) fragment.message(msg, *args)
            }
            pop()
        }
    }

    fun sendMessage(item: RachelTab, msg: RachelMessage, vararg args: Any?) = fragments[item.index]?.message(msg, *args)

    @Suppress("UNCHECKED_CAST")
    fun <T> sendMessageForResult(item: RachelTab, msg: RachelMessage, vararg args: Any?): T? = fragments[item.index]?.messageForResult(msg, *args) as T?

    fun <T : RachelFragment<*>> sendBottomMessage(cls: KClass<T>, msg: RachelMessage, vararg args: Any?) {
        val fragmentCount = fragmentStack.size
        if (fragmentCount > 0) {
            if (fragmentCount > 1) {
                val fragment = fragmentStack[fragmentCount - 2]
                if (cls.isInstance(fragment)) fragment.message(msg, *args)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : RachelFragment<*>, R> sendBottomMessageForResult(cls: KClass<T>, msg: RachelMessage, vararg args: Any?): R? {
        val fragmentCount = fragmentStack.size
        if (fragmentCount > 0) {
            if (fragmentCount > 1) {
                val fragment = fragmentStack[fragmentCount - 2]
                if (cls.isInstance(fragment)) return fragment.messageForResult(msg, *args) as R?
            }
        }
        return null
    }

    fun goBack() = currentFragment.back()

    private fun processSchemeContent(uri: Uri) {
        // TODO
        // navigate(FragmentImportMod(this, uri))
    }

    private fun processSchemeRachel(uri: Uri, args: HashMap<String, String>) {
//        when (uri.path) {
//            "/openProfile" -> {
//                args["uid"]?.toIntOrNull()?.apply { navigate(FragmentProfile(this@RachelPages, this)) }
//            }
//        }
    }

    private fun processActionView(uri: Uri) {
        val args = HashMap<String, String>()
        for (name in uri.queryParameterNames) uri.getQueryParameter(name)?.apply { args[name] = this }
        when (uri.scheme) {
            "content" -> processSchemeContent(uri)
            "rachel" -> processSchemeRachel(uri, args)
        }
    }

    private fun processActionMain() { }

    fun processUri(uri: Uri) {
        val args = HashMap<String, String>()
        for (name in uri.queryParameterNames) uri.getQueryParameter(name)?.apply { args[name] = this }
        processSchemeRachel(uri, args)
    }

    fun processIntent(intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_MAIN -> processActionMain()
                Intent.ACTION_VIEW -> {
                    intent.data?.apply { processActionView(this) }
                }
                Intent.ACTION_SEND -> { }
            }
        }
        catch (ignored: Exception) { }
    }

    fun getResString(@StringRes id: Int) = context.getString(id)

    fun getResColor(@ColorRes id: Int) = context.getColor(id)

    fun getResDimension(@DimenRes id: Int) = context.resources.getDimension(id)

    // 取当前版本
    val appVersion: Long get() = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    fun appVersionName(version: Long): String = "${version / 100}.${version / 10 % 10}.${version % 10}"
}