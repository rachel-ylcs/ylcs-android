package com.yinlin.rachel.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.yinlin.rachel.clearAddAll
import com.yinlin.rachel.rachelClick
import java.util.Collections


abstract class RachelAdapter<Binding : ViewBinding, Item> : RecyclerView.Adapter<RachelAdapter.RachelViewHolder<Binding>>() {
    class RachelViewHolder<Binding : ViewBinding>(val v: Binding) : RecyclerView.ViewHolder(v.root)

    protected abstract fun bindingClass(): Class<Binding>
    protected open fun init(holder: RachelViewHolder<Binding>, v: Binding) { }
    protected open fun update(v: Binding, item: Item, position: Int) { }
    protected open fun onItemClicked(v: Binding, item: Item, position: Int) { }
    protected open fun onItemLongClicked(v: Binding, item: Item, position: Int) { }

    val items = mutableListOf<Item>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RachelViewHolder<Binding> {
        val cls: Class<Binding> = bindingClass()
        val method = cls.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.javaPrimitiveType)
        @Suppress("UNCHECKED_CAST")
        val v = method.invoke(null, LayoutInflater.from(parent.context), parent, false) as Binding
        val holder = RachelViewHolder(v)
        val root = v.root
        root.rachelClick(300) {
            val position = holder.bindingAdapterPosition
            onItemClicked(holder.v, items[position], position)
        }
        root.setOnLongClickListener {
            val position = holder.bindingAdapterPosition
            onItemLongClicked(holder.v, items[position], position)
            true
        }
        init(holder, v)
        return holder
    }

    override fun onBindViewHolder(holder: RachelViewHolder<Binding>, position: Int) = update(holder.v, items[position], position)

    override fun getItemCount() = items.size

    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val size: Int get() = items.size

    operator fun get(position: Int) = items[position]
    operator fun set(position: Int, item: Item) { items[position] = item }

    operator fun plusAssign(item: Item) { items.add(item) }
    fun addItem(index: Int, item: Item) = items.add(index, item)
    inline fun findItem(predicate: (Item) -> Boolean) = items.indexOfFirst(predicate)
    fun removeItem(index: Int) = items.removeAt(index)
    fun swapItem(src: Int, des: Int) = Collections.swap(items, src, des)

    fun setSource(items: List<Item>) = this.items.clearAddAll(items)
    fun addSource(items: List<Item>) = this.items.addAll(items)
    fun clearSource() = items.clear()
    inline fun mapSource(action: (Item) -> Unit) = items.forEach(action)
    inline fun allSource(predicate: (Item) -> Boolean) = items.all(predicate)
    inline fun filterSource(predicate: (Item) -> Boolean) = items.filter(predicate)

    @SuppressLint("NotifyDataSetChanged")
    fun notifySource() = notifyDataSetChanged()

    interface ListTouch<Item> {
        fun onMove(item1: Item, position1: Int, item2: Item, position2: Int) { }
        fun onMoved() { }
        fun onRemove(item: Item, position: Int) { }
    }

    fun setListTouch(view: RecyclerView, canDrag: Boolean = true, canSwipe: Boolean = true, callback: ListTouch<Item>) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            var startPos: Int = -1
            var endPos: Int = -1

            override fun isLongPressDragEnabled() = canDrag
            override fun isItemViewSwipeEnabled() = canSwipe
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.75f
            override fun getMovementFlags(view: RecyclerView, holder: RecyclerView.ViewHolder) =
                makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT)

            override fun onMove(view: RecyclerView, src: RecyclerView.ViewHolder, des: RecyclerView.ViewHolder): Boolean {
                val srcPos = src.bindingAdapterPosition
                val desPos = des.bindingAdapterPosition
                callback.onMove(items[srcPos], srcPos, items[desPos], desPos)
                Collections.swap(items, srcPos, desPos)
                notifyItemMoved(srcPos, desPos)
                endPos = desPos
                return true
            }

            override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(holder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (startPos != endPos) callback.onMoved()
                    startPos = -1
                    endPos = -1
                }
                else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    val pos = holder?.bindingAdapterPosition ?: -1
                    startPos = pos
                    endPos = pos
                }
            }

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val pos = holder.bindingAdapterPosition
                val item = items[pos]
                items.removeAt(pos)
                notifyItemRemoved(pos)
                callback.onRemove(item, pos)
            }
        })
        itemTouchHelper.attachToRecyclerView(view)
    }
}