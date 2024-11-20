package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.forjrking.lubankt.Luban
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.engine.CompressFileEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.yinlin.rachel.R
import com.yinlin.rachel.databinding.ItemImageSelectBinding
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.model.RachelPictureSelector.RachelImageEngine
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.visible


class ImageSelectView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {
    interface Listener {
        fun onOverflow(maxNum: Int)
        fun onImageClicked(position: Int, images: List<RachelPreview>)
    }

    class ViewHolder(val v: ItemImageSelectBinding) : RecyclerView.ViewHolder(v.root)

    class Adapter(private val rv: ImageSelectView) : RecyclerView.Adapter<ViewHolder>() {
        internal val items = mutableListOf<String>()

        private val compressEngine = CompressFileEngine { context, source, call ->
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
            else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            Luban.with(context as FragmentActivity).load(source)
                .concurrent(true).useDownSample(true)
                .format(format).ignoreBy(rv.mMaxFileSize.toLong()).quality(rv.mQuality).compressObserver {
                    onSuccess = {
                        val count = it.size
                        source.forEachIndexed { index, pic ->
                            if (index < count) call.onCallback(pic.toString(), it[index].absolutePath)
                            else call.onCallback(pic.toString(), null)
                        }
                    }
                    onError = { _, pic -> call.onCallback(pic.toString(), null) }
                }.launch()
        }

        private val resultProcessor = object : OnResultCallbackListener<LocalMedia> {
            override fun onResult(result: ArrayList<LocalMedia>) {
                val currentSize = items.size
                val addSize = result.size
                for (pic in result) items += pic.compressPath
                notifyItemRangeChanged(currentSize, addSize + 1)
            }
            override fun onCancel() {}
        }

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
                        PictureSelector.create(context).openGallery(SelectMimeType.ofImage())
                            .setImageEngine(RachelImageEngine.instance)
                            .setSelectionMode(SelectModeConfig.MULTIPLE)
                            .setMaxSelectNum(addNum)
                            .setCompressEngine(compressEngine)
                            .forResult(resultProcessor)
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
                Glide.with(v.root.context).load(R.drawable.img_add_image).into(v.pic)
            }
            else { // 是图片
                v.delete.visible = true
                Glide.with(v.root.context).load(items[position]).into(v.pic)
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
}