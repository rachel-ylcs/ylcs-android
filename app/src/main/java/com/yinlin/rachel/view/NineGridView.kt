package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.R
import com.yinlin.rachel.clearAddAll
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.toDP

class NineGridView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    class Adapter(
        private val context: Context,
        private val ngv: NineGridView,
        val items: MutableList<String> = mutableListOf()
    ) : RecyclerView.Adapter<ViewHolder>() {
        private val rilNet: RachelImageLoader = RachelImageLoader(context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val iv = ImageView(context)
            val holder = ViewHolder(iv)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setPadding(ngv.padding, ngv.padding, ngv.padding, ngv.padding)
            iv.rachelClick { ngv.listener(items[holder.bindingAdapterPosition]) }
            return holder
        }

        override fun getItemCount() = items.size.coerceAtMost(9)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as ImageView).apply {
                val width = ngv.measuredWidth / ngv.column
                layoutParams = ViewGroup.LayoutParams(width, width)
                load(rilNet, items[position])
            }
        }
    }

    class Manager(context: Context, column: Int) : GridLayoutManager(context, column) {
        override fun canScrollVertically() = false
    }

    private var column: Int = 1

    var padding: Int = 2.toDP(context)

    var images: List<String>
        get() = (adapter as Adapter).items
        set(value) {
            (adapter as Adapter).apply {
                items.clearAddAll(value)
                column = when (value.size) {
                    in 0..1 -> 1
                    in 2..4 -> 2
                    else -> 3
                }
                layoutManager = Manager(context, column)
            }
        }

    var listener: (String) -> Unit = {}

    init {
        layoutManager = Manager(context, column)
        adapter = Adapter(context, this)
    }
}