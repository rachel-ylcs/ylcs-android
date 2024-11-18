package com.yinlin.rachel.model

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.tip

abstract class RachelFragment<Binding : ViewBinding>(val main: MainActivity) : Fragment() {
    private var _binding: Binding? = null
    val v: Binding get() = _binding!!

    private var isFirstStart = true

    protected abstract fun bindingClass(): Class<Binding>
    protected open fun init() { }
    protected open fun start() { }
    protected open fun update() { }
    protected open fun hidden() { }
    protected open fun quit() { }
    open fun back(): Boolean = false
    open fun message(msg: RachelMessage, vararg args: Any?) { }
    open fun messageForResult(msg: RachelMessage, vararg args: Any?): Any? = null

    final override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View {
        val cls: Class<Binding> = bindingClass()
        val method = cls.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.javaPrimitiveType)
        @Suppress("UNCHECKED_CAST")
        _binding = method.invoke(null, inflater, parent, false) as Binding
        return v.root
    }

    final override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, null)
        init()
    }

    final override fun onHiddenChanged(isHidden: Boolean) {
        super.onHiddenChanged(isHidden)
        if (isHidden) hidden()
        else {
            if (isFirstStart) {
                isFirstStart = false
                start()
            }
            else update()
        }
    }

    final override fun onDestroyView() {
        quit()
        super.onDestroyView()
        _binding = null
        isFirstStart = true
    }

    final override fun onStart() {
        super.onStart()
        main.updateTabStatus()
    }

    fun onActivityStart() {
        if (isFirstStart) {
            isFirstStart = false
            start()
        }
        else update()
    }

    fun onActivityStop() {
        hidden()
    }

    val isAttached: Boolean get() = _binding != null
    fun post(r: Runnable) = main.handler.post(r)
    fun postDelay(delay: Long, r: Runnable) = main.handler.postDelayed(r, delay)
    fun removePost(r: Runnable) = main.handler.removeCallbacks(r)

    fun tip(type: Tip, text: String, anchorView: View? = null) = main.tip(type, text, anchorView)
}