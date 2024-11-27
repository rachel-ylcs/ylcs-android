package com.yinlin.rachel.fragment

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.databinding.FragmentWeiboAlbumBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.toDP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.internal.Ref.IntRef

class FragmentWeiboAlbum(main: MainActivity, private val containerId: String) : RachelFragment<FragmentWeiboAlbumBinding>(main) {
    class ViewHolder(val view: ImageView) : RecyclerView.ViewHolder(view)

    class Adapter(private val main: MainActivity) : RecyclerView.Adapter<ViewHolder>() {
        var pics = listOf<RachelPreview>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            val width = parent.measuredWidth / 4
            val view = ImageView(context)
            val holder = ViewHolder(view)
            view.layoutParams = ViewGroup.LayoutParams(width, width)
            val padding = 5.toDP(context)
            view.setPadding(padding, padding, padding, padding)
            view.rachelClick {
                val pos = holder.bindingAdapterPosition
                main.navigate(FragmentImagePreview(main, pics, pos))
            }
            return holder
        }

        override fun getItemCount(): Int = pics.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.view.load(pics[position].mImageUrl)
        }
    }

    data class AlbumCache(val count: Int, val items: List<RachelPreview>)

    companion object {
        const val PIC_LIMIT = 24
        const val PIC_MAX_LIMIT = 1000
    }

    private var current: Int = 0
    private val album = MutableList<AlbumCache?>(PIC_MAX_LIMIT) { null }
    private var maxNum: Int = -1
    private val mAdapter = Adapter(main)

    override fun bindingClass() = FragmentWeiboAlbumBinding::class.java

    override fun init() {
        v.list.apply {
            layoutManager = GridLayoutManager(context, 4)
            setItemViewCacheSize(0)
            recycledViewPool.setMaxRecycledViews(0, PIC_LIMIT)
            adapter = mAdapter
        }

        v.previous.rachelClick {
            if (current > 1) requestAlbum(current - 1)
            else tip(Tip.WARNING, "已经是最后一页啦")
        }

        v.next.rachelClick {
            if ((maxNum == -1 || current < maxNum) && current < PIC_MAX_LIMIT - 1) requestAlbum(current + 1)
            else tip(Tip.WARNING, "已经是最后一页啦")
        }

        requestAlbum(1)
    }

    override fun back() = true

    @NewThread @SuppressLint("NotifyDataSetChanged")
    private fun requestAlbum(page: Int) {
        lifecycleScope.launch {
            if (album[page - 1] == null) { // 无缓存
                val picCount = IntRef()
                v.title.loading = true
                val pagePics = withContext(Dispatchers.IO) { WeiboAPI.extractWeiboUserAlbumPics(containerId, page, PIC_LIMIT, picCount) }
                v.title.loading = false
                if (pagePics.isNotEmpty()) album[page - 1] = AlbumCache(picCount.element, pagePics)
            }
            val currentAlbum = album[page - 1]
            if (currentAlbum != null) {
                v.title.text = "共 ${currentAlbum.count} 张"
                current = page
                v.page.text = "第 $page 页"
                mAdapter.pics = currentAlbum.items
                mAdapter.notifyDataSetChanged()
            }
            else {
                maxNum = page - 1
                tip(Tip.WARNING, "已经是最后一页啦")
            }
        }
    }
}