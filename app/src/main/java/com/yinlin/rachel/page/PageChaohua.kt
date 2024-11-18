package com.yinlin.rachel.page

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.databinding.PageChaohuaBinding
import com.yinlin.rachel.fragment.FragmentMsg
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.page.common.WeiboAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PageChaohua(fragment: FragmentMsg) : RachelViewPage<PageChaohuaBinding, FragmentMsg>(fragment) {
    private val adapter = WeiboAdapter(fragment.main)
    private var sinceId: Long = 0L

    override fun bindingClass(): Class<PageChaohuaBinding> = PageChaohuaBinding::class.java

    override fun init() {
        // 刷新与加载
        v.container.apply {
            setEnableAutoLoadMore(true)
            setEnableOverScrollDrag(false)
            setEnableOverScrollBounce(false)
            setOnRefreshListener { requestNewData() }
            setOnLoadMoreListener { requestMoreData() }
        }

        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(fragment.main)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 16)
            setItemViewCacheSize(4)
            adapter = this@PageChaohua.adapter
        }

        requestNewData()
    }

    override fun back(): Boolean {
        v.list.smoothScrollToPosition(0)
        return false
    }

    @NewThread
    fun requestNewData() {
        lifecycleScope.launch {
            v.container.setNoMoreData(false)
            v.state.showLoading()
            v.container.setEnableLoadMore(false)
            adapter.clearSource()
            sinceId = withContext(Dispatchers.IO) { WeiboAPI.getChaohua(0, adapter.items) }
            if (adapter.isEmpty) v.state.showOffline { requestNewData() }
            else v.state.showContent()
            if (v.container.isRefreshing) {
                if (sinceId == 0L) v.container.finishRefreshWithNoMoreData()
                else v.container.finishRefresh()
            }
            adapter.notifySource()
            v.container.setEnableLoadMore(true)
        }
    }

    @NewThread
    fun requestMoreData() {
        lifecycleScope.launch {
            val oldSize = adapter.size
            sinceId = withContext(Dispatchers.IO) { WeiboAPI.getChaohua(sinceId, adapter.items) }
            if (sinceId == 0L) v.container.finishLoadMoreWithNoMoreData()
            else {
                adapter.notifyItemRangeInserted(oldSize, adapter.size - oldSize)
                v.container.finishLoadMore()
            }
        }
    }
}