package com.yinlin.rachel.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.textColor
import com.yinlin.rachel.toDP


@SuppressLint("NotifyDataSetChanged")
class BreadCrumbView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    class Adapter(private val rv: BreadCrumbView) : RecyclerView.Adapter<ViewHolder>() {
        val items = mutableListOf(rv.mHome)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            val tv = TextView(context)
            tv.layoutParams = MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT).apply {
                val margin = 10.toDP(context)
                setMargins(margin, margin, margin, margin)
            }
            val holder = ViewHolder(tv)
            tv.rachelClick {
                val oldSize = items.size
                val position = holder.bindingAdapterPosition
                if (position != oldSize - 1) {
                    for (i in (position + 1)..< oldSize) items.removeLast()
                    notifyDataSetChanged()
                    rv.listener(oldSize - 1, position, items[position])
                }
            }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tv = holder.itemView as TextView
            val context = tv.context
            tv.text = items[position]
            tv.textColor = context.getColor(if (position == items.size - 1) R.color.steel_blue else R.color.black)
        }

        override fun getItemCount() = items.size
    }

    class TabDecoration(context: Context) : ItemDecoration() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val iconWidth = 10.toDP(context)
        private val bitmap = AppCompatResources.getDrawable(context, R.drawable.svg_expand_gray)!!.toBitmap(iconWidth, iconWidth)

        override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
            val childCount = parent.childCount
            val offset = iconWidth / 2f
            val top = (parent.height - iconWidth) / 2f
            for (i in 0 until childCount - 1) {
                val view = parent.getChildAt(i)
                canvas.drawBitmap(bitmap, view.right + offset, top, paint)
            }
        }
    }

    private val mHome: String
    private val mAdapter: Adapter
    var listener: (Int, Int, String) -> Unit = { _, _, _ -> }

    init {
        RachelAttr(context, attrs, R.styleable.BreadCrumbView).use {
            mHome = it.value(R.styleable.BreadCrumbView_Home, "首页")
        }

        setPadding(10.toDP(context), paddingTop, 10.toDP(context), paddingBottom)
        mAdapter = Adapter(this)
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        addItemDecoration(TabDecoration(context))
        adapter = mAdapter
    }

    fun addItem(item: String) {
        val items = mAdapter.items
        val oldSize = items.size
        items.add(item)
        mAdapter.notifyDataSetChanged()
        listener(oldSize - 1, oldSize, item)
    }

    fun backItem() {
        val items = mAdapter.items
        val oldSize = items.size
        if (oldSize > 1) {
            items.removeLast()
            mAdapter.notifyDataSetChanged()
            listener(oldSize - 1, oldSize - 2, items[oldSize - 2])
        }
    }

    fun clearItem() {
        val items = mAdapter.items
        val oldSize = items.size
        items.clear()
        items.add(mHome)
        mAdapter.notifyDataSetChanged()
        listener(oldSize - 1, 0, items[0])
    }
}