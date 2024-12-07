package com.yinlin.rachel.page

import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.res.ResFile
import com.yinlin.rachel.data.res.ResFolder
import com.yinlin.rachel.databinding.ItemPhotoBinding
import com.yinlin.rachel.databinding.PagePhotoBinding
import com.yinlin.rachel.fragment.FragmentMsg
import com.yinlin.rachel.fragment.FragmentPhotoPreview
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelImageLoader.loadLoading
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.tool.visible

class PagePhoto(fragment: FragmentMsg) : RachelViewPage<PagePhotoBinding, FragmentMsg>(fragment) {
    class Adapter(private val page: PagePhoto, var currentRes: ResFolder) : RachelAdapter<ItemPhotoBinding, ResFile>() {
        override fun bindingClass() = ItemPhotoBinding::class.java

        override fun update(v: ItemPhotoBinding, item: ResFile, position: Int) {
            v.name.text = item.name
            if (item is ResFolder) {
                v.author.text = ""
                v.author.visible = false
                v.pic.load(R.drawable.img_photo_album)
            } else {
                v.author.text = item.author
                v.author.visible = true
                v.pic.loadLoading(item.thumbUrl ?: "")
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
    private val mAdapter = Adapter(this, rootRes)

    override fun bindingClass(): Class<PagePhotoBinding> = PagePhotoBinding::class.java

    override fun init() {
        // 列表
        v.list.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 15)
            adapter = mAdapter
        }

        // Tab
        v.tab.listener = { oldPos, newPos, text ->
            mAdapter.currentRes = if (newPos == 0) rootRes
            else if (oldPos > newPos) {
                var root = mAdapter.currentRes
                for (i in newPos ..< oldPos) root = root.parent!!
                root
            }
            else mAdapter.currentRes.items.find { it.name == text }!! as ResFolder
            v.list.smoothScrollToPosition(0)
            mAdapter.setSource(mAdapter.currentRes.items)
            mAdapter.notifySource()
        }

        // 下拉刷新
        v.container.setOnRefreshListener { loadRes() }

        // 首次加载
        loadRes()
    }

    override fun back(): BackState {
        if (v.tab.backItem()) return BackState.CANCEL
        return BackState.HOME
    }

    @IOThread
    fun loadRes() {
        v.state.showLoading()
        startIOWithResult({ API.CommonAPI.getPhotos() }) {
            rootRes = it
            if (v.container.isRefreshing) v.container.finishRefresh()
            if (rootRes.items.isEmpty()) v.state.showOffline { loadRes() }
            else {
                v.state.showContent()
                v.tab.clearItem()
            }
        }
    }
}