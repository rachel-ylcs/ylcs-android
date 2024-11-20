package com.yinlin.rachel.page

import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.res.ResFile
import com.yinlin.rachel.data.res.ResFolder
import com.yinlin.rachel.databinding.ItemPhotoBinding
import com.yinlin.rachel.databinding.PagePhotoBinding
import com.yinlin.rachel.fragment.FragmentMsg
import com.yinlin.rachel.fragment.FragmentPhotoPreview
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PagePhoto(fragment: FragmentMsg) : RachelViewPage<PagePhotoBinding, FragmentMsg>(fragment) {
    class Adapter(private val page: PagePhoto, var currentRes: ResFolder) : RachelAdapter<ItemPhotoBinding, ResFile>() {
        private val rilNet = RachelImageLoader(page.fragment.main, R.drawable.placeholder_loading, DiskCacheStrategy.ALL)

        override fun bindingClass() = ItemPhotoBinding::class.java

        override fun update(v: ItemPhotoBinding, item: ResFile, position: Int) {
            v.name.text = item.name
            if (item is ResFolder) {
                v.author.text = ""
                v.author.visible = false
                v.pic.load(rilNet, R.drawable.img_photo_album)
            } else {
                v.author.text = item.author
                v.author.visible = true
                v.pic.load(rilNet, item.thumbUrl ?: "")
            }
        }

        override fun onItemClicked(v: ItemPhotoBinding, item: ResFile, position: Int) {
            if (item is ResFolder) page.v.tab.addItem(item.name)
            else {
                val main = page.fragment.main
                if (item.thumbUrl != null) main.navigate(FragmentPhotoPreview(main, RachelPreview(item.thumbUrl!!, item.sourceUrl!!)))
            }
        }
    }

    private var rootRes = ResFolder.emptyRes
    private val adapter = Adapter(this, rootRes)

    override fun bindingClass(): Class<PagePhotoBinding> = PagePhotoBinding::class.java

    override fun init() {
        // 列表
        v.list.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 15)
            adapter = this@PagePhoto.adapter
        }

        // Tab
        v.tab.listener = { oldPos, newPos, text ->
            adapter.currentRes = if (newPos == 0) rootRes
            else if (oldPos > newPos) {
                var root = adapter.currentRes
                for (i in newPos ..< oldPos) root = root.parent!!
                root
            }
            else adapter.currentRes.items.find { it.name == text }!! as ResFolder
            v.list.smoothScrollToPosition(0)
            adapter.setSource(adapter.currentRes.items)
            adapter.notifySource()
        }

        // 下拉刷新
        v.container.setOnRefreshListener { loadRes() }

        // 首次加载
        loadRes()
    }

    override fun back(): Boolean {
        v.tab.backItem()
        return false
    }

    @NewThread
    fun loadRes() {
        lifecycleScope.launch {
            v.state.showLoading()
            rootRes = withContext(Dispatchers.IO) { API.CommonAPI.getPhotos() }
            if (v.container.isRefreshing) v.container.finishRefresh()
            if (rootRes.items.isEmpty()) v.state.showOffline { loadRes() }
            else {
                v.state.showContent()
                v.tab.clearItem()
            }
        }
    }
}