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
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick

class NineGridView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    class Adapter(
        private val context: Context,
        private val ngv: NineGridView,
        val items: MutableList<RachelPreview> = mutableListOf()
    ) : RecyclerView.Adapter<ViewHolder>() {
        private val rilNet: RachelImageLoader = RachelImageLoader(context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val iv = ImageView(context)
            val holder = ViewHolder(iv)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setPadding(ngv.gap, ngv.gap, ngv.gap, ngv.gap)
            iv.rachelClick {
                val position = holder.bindingAdapterPosition
                ngv.listener(position, items[position])
            }
            return holder
        }

        override fun getItemCount() = items.size.coerceAtMost(9)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val imageView = holder.itemView as ImageView
            val width = ngv.measuredWidth / ngv.column
            imageView.layoutParams = ViewGroup.LayoutParams(width, width)
            imageView.load(rilNet, items[position].mImageUrl)
        }
    }

    class Manager(context: Context, column: Int) : GridLayoutManager(context, column) {
        override fun canScrollVertically() = false
    }

    private var column: Int = 1
    private val mAdapter = Adapter(context, this)

    var gap: Int
    var listener: (Int, RachelPreview) -> Unit = { _, _, -> }

    var images: List<RachelPreview>
        get() = mAdapter.items
        set(value) {
            mAdapter.apply {
                items.clearAddAll(value)
                column = when (value.size) {
                    in 0..1 -> 1
                    in 2..4 -> 2
                    else -> 3
                }
                layoutManager = Manager(context, column)
            }
        }

    init {
        RachelAttr(context, attrs, R.styleable.NineGridView).use {
            gap = it.dp(R.styleable.NineGridView_Gap, 2)
        }

        layoutManager = Manager(context, column)
        setItemViewCacheSize(0)
        recycledViewPool.setMaxRecycledViews(0, 9)
        adapter = mAdapter
    }
}