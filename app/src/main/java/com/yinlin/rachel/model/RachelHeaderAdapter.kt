package com.yinlin.rachel.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.yinlin.rachel.annotation.HeaderLayout
import com.yinlin.rachel.annotation.HeaderLayout.Companion.inflateHeader
import com.yinlin.rachel.annotation.HeaderLayout.Companion.inflateItem
import com.yinlin.rachel.tool.clearAddAll
import com.yinlin.rachel.tool.meta
import com.yinlin.rachel.tool.rachelClick
import java.util.Collections

abstract class RachelHeaderAdapter<HeaderBinding : ViewBinding, ItemBinding : ViewBinding, Item> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val ITEM = 0
        private const val HEADER = 1
    }

    val items = mutableListOf<Item>()
    private var vHeader: HeaderBinding? = null
    val header: HeaderBinding get() = vHeader!!

    class RachelHeaderViewHolder<HeaderBinding : ViewBinding>(val v: HeaderBinding) : RecyclerView.ViewHolder(v.root)
    class RachelItemViewHolder<ItemBinding : ViewBinding>(val v: ItemBinding) : RecyclerView.ViewHolder(v.root) {
        val positionEx: Int get() = bindingAdapterPosition - 1
    }

    protected open fun init(holder: RachelItemViewHolder<ItemBinding>, v: ItemBinding) { }
    protected open fun update(v: ItemBinding, item: Item, position: Int) { }
    protected open fun onItemClicked(v: ItemBinding, item: Item, position: Int) { }
    protected open fun onItemLongClicked(v: ItemBinding, item: Item, position: Int) { }
    protected open fun initHeader(v: HeaderBinding) { }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) {
            if (vHeader == null) {
                vHeader = this.meta<HeaderLayout>()!!.inflateHeader(LayoutInflater.from(parent.context), parent)
                initHeader(header)
            }
            RachelHeaderViewHolder(header)
        }
        else {
            val v: ItemBinding = this.meta<HeaderLayout>()!!.inflateItem(LayoutInflater.from(parent.context), parent)
            val holder = RachelItemViewHolder(v)
            val root = v.root
            root.rachelClick {
                val position = holder.bindingAdapterPosition - 1
                onItemClicked(holder.v, items[position], position)
            }
            root.setOnLongClickListener {
                val position = holder.bindingAdapterPosition - 1
                onItemLongClicked(holder.v, items[position], position)
                true
            }
            init(holder, v)
            holder
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RachelItemViewHolder<*>) update(holder.v as ItemBinding, items[position - 1], position - 1)
    }

    override fun getItemViewType(position: Int) = if (position == 0) HEADER else ITEM

    override fun getItemCount() = items.size + 1

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val manager = recyclerView.layoutManager
        if (manager is GridLayoutManager) {
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (getItemViewType(position) == HEADER) manager.spanCount else 1
                }
            }
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val lp = holder.itemView.layoutParams
        if (lp is StaggeredGridLayoutManager.LayoutParams) lp.isFullSpan = holder.itemViewType == HEADER
    }

    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val size: Int get() = items.size

    operator fun get(position: Int) = items[position]
    operator fun set(position: Int, item: Item) { items[position] = item }

    operator fun plusAssign(item: Item) { items.add(item) }
    fun addItem(index: Int, item: Item) = items.add(index, item)
    inline fun findItem(predicate: (Item) -> Boolean) = items.indexOfFirst(predicate)
    fun removeItem(index: Int) = items.removeAt(index)
    fun moveItem(src: Int, des: Int) {
        val element = items[src]
        items.removeAt(src)
        items.add(des, element)
    }
    fun swapItem(src: Int, des: Int) = Collections.swap(items, src, des)

    fun setSource(items: List<Item>) = this.items.clearAddAll(items)
    fun addSource(items: List<Item>) = this.items.addAll(items)
    fun clearSource() = items.clear()
    inline fun mapSource(action: (Item) -> Unit) = items.forEach(action)
    inline fun allSource(predicate: (Item) -> Boolean) = items.all(predicate)
    inline fun filterSource(predicate: (Item) -> Boolean) = items.filter(predicate)

    fun notifySourceEx() {
        if (items.size > 0) notifyItemRangeChanged(1, items.size)
    }

    fun notifyChangedEx(position: Int) {
        if (items.size > 0) notifyItemChanged(position + 1)
    }

    fun notifyChangedEx(position: Int, count: Int) {
        if (items.size > 0) notifyItemRangeChanged(position + 1, count)
    }

    fun notifyInsertEx(position: Int) {
        if (items.size > 0) notifyItemInserted(position + 1)
    }

    fun notifyInsertEx(position: Int, count: Int) {
        if (items.size > 0) notifyItemRangeInserted(position + 1, count)
    }

    fun notifyRemovedEx(position: Int) {
        notifyItemRemoved(position + 1)
    }

    fun notifyRemovedEx(position: Int, count: Int) {
        notifyItemRangeRemoved(position + 1, count)
    }

    fun notifyMovedEx(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition + 1, toPosition + 1)
    }
}