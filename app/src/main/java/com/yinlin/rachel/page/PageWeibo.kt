package com.yinlin.rachel.page

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.databinding.PageWeiboBinding
import com.yinlin.rachel.fragment.FragmentMsg
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.common.WeiboAdapter
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.isTop

class PageWeibo(fragment: FragmentMsg) : RachelViewPage<PageWeiboBinding, FragmentMsg>(fragment) {
    private val mAdapter = WeiboAdapter(fragment.main)

    override fun bindingClass(): Class<PageWeiboBinding> = PageWeiboBinding::class.java

    override fun init() {
        // 刷新与加载
        v.container.apply {
            setEnableOverScrollDrag(false)
            setEnableOverScrollBounce(false)
            setOnRefreshListener { requestNewData() }
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
        v.state.showLoading()
        mAdapter.clearSource()
        startIOWithResult({ WeiboAPI.extractAllUserWeibo(Config.weibo_users, mAdapter.items) }) {
            if (mAdapter.isEmpty) v.state.showOffline { requestNewData() }
            else v.state.showContent()
            if (v.container.isRefreshing) v.container.finishRefresh()
            mAdapter.notifySource()
        }
    }
}