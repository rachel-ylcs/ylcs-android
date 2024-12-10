package com.yinlin.rachel.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.annotation.Layout.Companion.inflate
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.meta
import com.yinlin.rachel.tool.startIO
import com.yinlin.rachel.tool.startIOWithResult
import kotlinx.coroutines.CoroutineScope

abstract class RachelViewPage<Binding : ViewBinding, F : RachelFragment<*>>(val fragment: F) {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class Adapter(private val pages: Array<RachelViewPage<*, *>>) : RecyclerView.Adapter<ViewHolder>() {
        companion object {
            fun ViewPager2.back(): BackState = (this.adapter as Adapter).pages[this.currentItem].back()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val page = pages[viewType]
            page._binding = page.meta<Layout>()!!.inflate(LayoutInflater.from(parent.context), parent)
            page.init()
            return ViewHolder(page.v.root)
        }

        override fun getItemViewType(position: Int) = position
        override fun getItemCount(): Int = pages.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) { }
    }

    private var _binding: Binding? = null
    val v: Binding get() = _binding!!

    protected open fun init() { }
    open fun back(): BackState = BackState.CANCEL

    fun startIO(block: suspend CoroutineScope.() -> Unit) = fragment.startIO(block)
    fun <T> startIOWithResult(ioBlock: suspend CoroutineScope.() -> T, mainBlock: suspend CoroutineScope.(T) -> Unit) = fragment.startIOWithResult(ioBlock, mainBlock)
}