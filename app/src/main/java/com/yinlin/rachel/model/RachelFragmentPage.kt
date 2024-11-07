package com.yinlin.rachel.model

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.adapter.FragmentStateAdapter

abstract class RachelFragmentPage<Binding : ViewBinding>(val pages: RachelPages) : Fragment() {
    class Adapter(activity: FragmentActivity, private val fragments: Array<RachelFragmentPage<*>>)
        : FragmentStateAdapter(activity) {
        override fun getItemCount() = fragments.size
        override fun createFragment(position: Int) = fragments[position]
    }

    private var _binding: Binding? = null
    val v: Binding get() = _binding!!

    protected abstract fun bindingClass(): Class<Binding>
    protected open fun init() { }
    open fun update() { }
    open fun hidden() { }
    protected open fun quit() { }

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

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onPause() {
        super.onPause()
        hidden()
    }

    final override fun onDestroyView() {
        quit()
        super.onDestroyView()
    }

    final override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}