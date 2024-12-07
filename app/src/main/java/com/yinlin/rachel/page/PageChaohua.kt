package com.yinlin.rachel.page

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.databinding.PageChaohuaBinding
import com.yinlin.rachel.fragment.FragmentMsg
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.common.WeiboAdapter
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.isTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PageChaohua(fragment: FragmentMsg) : RachelViewPage<PageChaohuaBinding, FragmentMsg>(fragment) {
    private val mAdapter = WeiboAdapter(fragment.main)
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
            adapter = mAdapter
        }

        requestNewData()
    }

    override fun back(): BackState {
        if (v.list.isTop) return BackState.HOME
        else {
            v.list.smoothScrollToPosition(0)
            return BackState.CANCEL
        }
    }

    @IOThread
    fun requestNewData() {
        lifecycleScope.launch {
            v.container.setNoMoreData(false)
            v.state.showLoading()
            v.container.setEnableLoadMore(false)
            mAdapter.clearSource()
            sinceId = withContext(Dispatchers.IO) { WeiboAPI.extractChaohua(0, mAdapter.items) }
            if (mAdapter.isEmpty) v.state.showOffline { requestNewData() }
            else v.state.showContent()
            if (v.container.isRefreshing) {
                if (sinceId == 0L) v.container.finishRefreshWithNoMoreData()
                else v.container.finishRefresh()
            }
            mAdapter.notifySource()
            v.container.setEnableLoadMore(true)
        }
    }

    @IOThread
    fun requestMoreData() {
        lifecycleScope.launch {
            val oldSize = mAdapter.size
            sinceId = withContext(Dispatchers.IO) { WeiboAPI.extractChaohua(sinceId, mAdapter.items) }
            if (sinceId == 0L) v.container.finishLoadMoreWithNoMoreData()
            else {
                mAdapter.notifyItemRangeInserted(oldSize, mAdapter.size - oldSize)
                v.container.finishLoadMore()
            }
        }
    }
}