package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.R
import com.yinlin.rachel.databinding.ItemImageSelectBinding
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelPictureSelector
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.visible
import java.io.File


class ImageSelectView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {
    interface Listener {
        fun onOverflow(maxNum: Int)
        fun onImageClicked(position: Int, images: List<RachelPreview>)
    }

    class ViewHolder(val v: ItemImageSelectBinding) : RecyclerView.ViewHolder(v.root)

    class Adapter(private val rv: ImageSelectView) : RecyclerView.Adapter<ViewHolder>() {
        internal val items = mutableListOf<String>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            val v = ItemImageSelectBinding.inflate(LayoutInflater.from(context), parent, false)
            val holder = ViewHolder(v)
            v.delete.rachelClick {
                val position = holder.bindingAdapterPosition
                if (position != items.size) { // 是图片
                    items.removeAt(position)
                    notifyItemRemoved(position)
                }
            }
            v.root.rachelClick {
                val position = holder.bindingAdapterPosition
                if (position == items.size) { // 是添加按钮
                    val addNum = maxOf(rv.mMaxNum - items.size, 0)
                    if (addNum == 0) rv.listener?.onOverflow(rv.mMaxNum)
                    else {
                        RachelPictureSelector.multiple(context, addNum, rv.mMaxFileSize, rv.mQuality) {
                            val currentSize = items.size
                            val addSize = it.size
                            items.addAll(it)
                            notifyItemRangeChanged(currentSize, addSize + 1)
                        }
                    }
                }
                else rv.listener?.onImageClicked(position, RachelPreview.fromSingleUri(items)) // 是图片
            }
            return holder
        }

        override fun getItemCount() = items.size + 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val v = holder.v
            if (position == items.size) { // 是添加按钮
                v.delete.visible = false
                v.pic.load(R.drawable.img_add_image)
            }
            else { // 是图片
                v.delete.visible = true
                v.pic.load(File(items[position]))
            }
        }
    }

    private val mMaxFileSize: Int
    private val mQuality: Int
    private val mMaxNum: Int
    private val mAdapter = Adapter(this)

    var listener: Listener? = null

    init {
        RachelAttr(context, attrs, R.styleable.ImageSelectView).use {
            mMaxFileSize = it.value(R.styleable.ImageSelectView_MaxFileSize, 150)
            mQuality = it.value(R.styleable.ImageSelectView_Quality, 90)

            val column = it.value(R.styleable.ImageSelectView_Column, 4).coerceAtLeast(1)
            val maxNum = it.value(R.styleable.ImageSelectView_MaxNum, 9)
            mMaxNum = if (maxNum <= 0) 9 else maxNum

            layoutManager = GridLayoutManager(context, if (column <= 0) 4 else column)
            recycledViewPool.setMaxRecycledViews(0, mMaxNum)
            setItemViewCacheSize(mMaxNum / 10)
        }

        adapter = mAdapter
        setHasFixedSize(true)
    }

    val images: List<String> get() = mAdapter.items
    val count: Int get() = mAdapter.items.size
    val isEmpty: Boolean get() = mAdapter.items.isEmpty()
    val isNotEmpty: Boolean get() = mAdapter.items.isNotEmpty()
}