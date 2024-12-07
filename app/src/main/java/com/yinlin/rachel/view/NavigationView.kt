package com.yinlin.rachel.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.ri
import com.yinlin.rachel.tool.textSizePx
import com.yinlin.rachel.tool.toDP


class NavigationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {
    class ItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
        : AppCompatTextView(context, attrs, defStyleAttr) {
        private val mDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.rc(R.color.light_gray)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f.toDP(context)
        }

        var icon: Drawable? = null
        var gap: Int = 5.toDP(context)
        var active: Boolean = false
            set(value) {
                field = value
                isSelected = value
                paint.isFakeBoldText = value
                invalidate()
            }
        var hasDivider: Boolean = true

        init {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = measuredWidth
            val h = measuredHeight
            if (active) {
                icon?.apply {
                    val iconSize = (2 * gap * 0.8f).toInt()
                    val left = (w - iconSize) / 2
                    val top = h - 2 * gap
                    setBounds(left, top, left + iconSize, top + iconSize)
                    draw(canvas)
                }
            }
            if (hasDivider) canvas.drawLine(w.toFloat(), h * 0.2f, w.toFloat(), h * 0.8f, mDividerPaint)
        }
    }

    data class Item(var title: String, var obj: Any? = null)

    interface Listener {
        fun onSelected(position: Int, title: String, obj: Any?)
        fun onItemLongClicked(position: Int, title: String, obj: Any?) { }
    }

    class ViewHolder(val view: ItemView) : RecyclerView.ViewHolder(view)

    class Adapter(
        private val mTextSize: Float,
        private val mTextColor: ColorStateList,
        private val mIcon: Drawable?,
        private val mGap: Int,
        private val mHasDivider: Boolean) : RecyclerView.Adapter<ViewHolder>() {
        val items = mutableListOf<Item>()
        var current: Int = -1
        var listener: Listener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = ItemView(parent.context)
            view.textSizePx = mTextSize
            view.setTextColor(mTextColor)
            view.setPadding(mGap * 2, mGap, mGap * 2, if (mIcon != null) mGap * 2 else mGap)
            view.gap = mGap
            view.icon = mIcon
            view.hasDivider = mHasDivider
            val holder = ViewHolder(view)
            view.rachelClick { selectItem(holder.bindingAdapterPosition) }
            view.setOnLongClickListener {
                val position = holder.bindingAdapterPosition
                val currentItem = items[position]
                listener?.onItemLongClicked(position, currentItem.title, currentItem.obj)
                true
            }
            return holder
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val view = holder.view
            val item = items[position]
            view.text = item.title
            view.active = position == current
        }

        fun selectItem(position: Int): Boolean {
            if (position in 0 ..< items.size && position != current) {
                val oldPosition = current
                current = position
                if (oldPosition != -1) notifyItemChanged(oldPosition)
                notifyItemChanged(current)
                val currentItem = items[current]
                listener?.onSelected(current, currentItem.title, currentItem.obj)
                return true
            }
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        fun setItems(data: Collection<Item>) {
            items.clear()
            items.addAll(data)
            current = if (items.isEmpty()) -1 else 0
            notifyDataSetChanged()
            if (items.isNotEmpty()) {
                val currentItem = items[0]
                listener?.onSelected(0, currentItem.title, currentItem.obj)
            }
        }

        fun addItem(item: Item, isSelected: Boolean) {
            val needUpdate = items.isEmpty() || isSelected
            items.add(item)
            val newPosition = items.size - 1
            notifyItemInserted(newPosition)
            if (needUpdate) {
                val oldPosition = current
                current = newPosition
                if (oldPosition != -1) notifyItemChanged(oldPosition)
                val currentItem = items[current]
                listener?.onSelected(current, currentItem.title, currentItem.obj)
            }
        }

        fun removeItem(position: Int) {
            if (position in 0..< items.size) {
                items.removeAt(position)
                notifyItemRemoved(position)
                if (current > position) current -= 1
                else if (current == position) {
                    if (position == items.size) current -= 1
                    if (current != -1) {
                        notifyItemChanged(current)
                        val currentItem = items[current]
                        listener?.onSelected(current, currentItem.title, currentItem.obj)
                    }
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun clearItems() {
            items.clear()
            current = -1
            notifyDataSetChanged()
        }
    }

    private val mAdapter: Adapter

    private val mTextSize: Float
    private val mTextColor: ColorStateList
    private val mIcon: Drawable?
    private val mGap: Int
    private val mBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val mHasDivider: Boolean

    var listener: Listener?
        get() = mAdapter.listener
        set(value) { mAdapter.listener = value }

    init {
        RachelAttr(context, attrs, R.styleable.NavigationView).use {
            mTextSize = it.spx(R.styleable.NavigationView_android_textSize, R.dimen.sm)
            val normalColor = it.color(R.styleable.NavigationView_NormalColor, R.color.black)
            val activeColor = it.color(R.styleable.NavigationView_ActiveColor, R.color.steel_blue)
            mTextColor = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
                intArrayOf(activeColor, normalColor)
            )
            val iconId = it.ref(R.styleable.NavigationView_android_icon, -1)
            mIcon = if (iconId == -1) null else context.ri(iconId)
            mGap = it.dp(R.styleable.NavigationView_Gap, 5).coerceAtLeast(5.toDP(context))
            mBorderPaint.color = it.color(R.styleable.NavigationView_BorderColor, R.color.steel_blue)
            mBorderPaint.strokeWidth = it.dp(R.styleable.NavigationView_BorderWidth, 1f)
            mHasDivider = it.value(R.styleable.NavigationView_HasDivider, true)
        }

        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        setHasFixedSize(true)
        setItemViewCacheSize(0)
        mAdapter = Adapter(mTextSize, mTextColor, mIcon, mGap, mHasDivider)
        adapter = mAdapter
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x1 = paddingStart.toFloat()
        val x2 = (measuredWidth - paddingEnd).toFloat()
        val y1 = paddingTop.toFloat()
        val y2 = (measuredHeight - paddingBottom).toFloat()
        canvas.drawLine(x1, y1, x2, y1, mBorderPaint)
        canvas.drawLine(x1, y2, x2, y2, mBorderPaint)
    }

    val isEmpty: Boolean get() = mAdapter.items.isEmpty()
    val size: Int get() = mAdapter.items.size

    var current: Int
        get() = mAdapter.current
        set(value) {
            if (mAdapter.selectItem(value)) smoothScrollToPosition(value)
        }

    val currentItem: Item? get() = mAdapter.items.getOrNull(mAdapter.current)

    var items: List<Item>
        get() = mAdapter.items
        set(value) {
            mAdapter.setItems(value)
        }

    var simpleItems: List<String>
        get() = mAdapter.items.map { it.title }
        set(value) {
            mAdapter.setItems(value.map { Item(it) })
        }

    fun addItem(item: Item, isSelected: Boolean = false) {
        mAdapter.addItem(item, isSelected)
        if (isSelected) smoothScrollToPosition(mAdapter.items.size - 1)
    }

    fun addItem(title: String, isSelected: Boolean = false, obj: Any? = null) = addItem(Item(title, obj), isSelected)

    fun removeItem(position: Int) = mAdapter.removeItem(position)

    fun clearItems() = mAdapter.clearItems()

    fun scrollToItem(position: Int) {
        if (position in 0 ..< mAdapter.items.size) smoothScrollToPosition(position)
    }

    inline fun withCurrent(callback: (position: Int, title: String, obj: Any?) -> Unit) {
        currentItem?.let { callback(current, it.title, it.obj) }
    }

    fun setItemTitle(position: Int, title: String) {
        if (position in 0 ..< mAdapter.items.size) {
            mAdapter.items[position].title = title
            mAdapter.notifyItemChanged(position)
        }
    }
}