package com.yinlin.rachel.page

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.Config
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.databinding.PageWeiboBinding
import com.yinlin.rachel.fragment.FragmentMsg
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.common.WeiboAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PageWeibo(fragment: FragmentMsg) : RachelViewPage<PageWeiboBinding, FragmentMsg>(fragment) {
    private val adapter = WeiboAdapter(fragment.main)

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
            adapter = this@PageWeibo.adapter
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
            v.state.showLoading()
            adapter.clearSource()
            withContext(Dispatchers.IO) { WeiboAPI.extractAllUserWeibo(Config.weibo_users, adapter.items) }
            if (adapter.isEmpty) v.state.showOffline { requestNewData() }
            else v.state.showContent()
            if (v.container.isRefreshing) v.container.finishRefresh()
            adapter.notifySource()
        }
    }
}