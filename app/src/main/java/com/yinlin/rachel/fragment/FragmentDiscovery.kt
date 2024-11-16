package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.topic.TopicPreview
import com.yinlin.rachel.databinding.FragmentDiscoveryBinding
import com.yinlin.rachel.databinding.ItemTopicUserBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.pureColor
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentDiscovery(pages: RachelPages) : RachelFragment<FragmentDiscoveryBinding>(pages)  {
    class Adapter(fragment: FragmentDiscovery) : RachelAdapter<ItemTopicUserBinding, TopicPreview>() {
        private val pages = fragment.pages
        private val rilNet = RachelImageLoader(pages.context, R.drawable.placeholder_loading, DiskCacheStrategy.ALL)

        override fun bindingClass() = ItemTopicUserBinding::class.java

        override fun init(holder: RachelViewHolder<ItemTopicUserBinding>, v: ItemTopicUserBinding) {
            v.avatar.rachelClick {
                val position = holder.bindingAdapterPosition
                pages.navigate(FragmentProfile(pages, this[position].uid))
            }
        }

        override fun update(v: ItemTopicUserBinding, item: TopicPreview, position: Int) {
            if (item.pic == null) v.pic.pureColor = 0
            else v.pic.load(rilNet, item.picPath)
            v.title.text = item.title
            v.name.text = item.name
            v.avatar.load(pages.ril, item.avatarPath)
            v.comment.text = item.commentNum.toString()
            v.coin.text = item.coinNum.toString()
        }

        override fun onItemClicked(v: ItemTopicUserBinding, item: TopicPreview, position: Int) {
            pages.navigate(FragmentTopic(pages, item.tid))
        }
    }

    companion object {
        const val TAB_LATEST = 0
        const val TAB_HOT = 1
    }

    private var topicUpper: Int = 2147483647
    private var topicOffset: Int = 0

    private val adapter = Adapter(this)

    override fun bindingClass() = FragmentDiscoveryBinding::class.java

    override fun init() {
        v.tab.addTabEx("最新")
        v.tab.addTabEx("热门")
        v.tab.selectTabEx(0)
        v.tab.listener = { position, _ ->
            when (position) {
                TAB_LATEST, TAB_HOT -> requestNewData()
                else -> { }
            }
        }

        v.buttonAdd.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeTopic) pages.navigate(FragmentCreateTopic(pages))
                else tip(Tip.WARNING, "你没有权限")
            }
            else tip(Tip.WARNING, "请先登录")
        }

        // 刷新与加载
        v.container.apply {
            setEnableAutoLoadMore(true)
            setEnableOverScrollDrag(false)
            setEnableOverScrollBounce(false)
            setEnableLoadMore(false)
            setOnRefreshListener { requestNewData() }
            setOnLoadMoreListener { requestNewDataMore() }
        }

        v.list.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
            setItemViewCacheSize(4)
            adapter = this@FragmentDiscovery.adapter
        }

        requestNewData()
    }

    override fun back(): Boolean {
        v.list.smoothScrollToPosition(0)
        return false
    }

    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            RachelMessage.DISCOVERY_ADD_TOPIC -> {
                // 只有在最新状态下更新, 热门无需更新
                if (v.tab.current == TAB_LATEST) {
                    adapter.addItem(0, args[0] as TopicPreview)
                    adapter.notifyItemInserted(0)
                    v.list.scrollToPosition(0)
                }
            }
            RachelMessage.DISCOVERY_DELETE_TOPIC -> {
                val tid = args[0] as Int
                val index = adapter.findItem { it.tid == tid }
                if (index != -1) {
                    adapter.removeItem(index)
                    adapter.notifyItemRemoved(index)
                }
            }
            else -> { }
        }
    }

    @NewThread
    private fun requestNewData() {
        v.container.setNoMoreData(false)
        lifecycleScope.launch {
            v.list.scrollToPosition(0)
            v.state.showLoading("加载主题中...")
            val current = v.tab.current
            val result = withContext(Dispatchers.IO) {
                if (current == TAB_LATEST) API.UserAPI.getLatestTopic()
                else API.UserAPI.getHotTopic()
            }
            when (result.code) {
                API.Code.SUCCESS -> {
                    val topics = result.data
                    if (topics.isEmpty()) {
                        if (current == TAB_LATEST) topicUpper = 2147483647
                        else topicOffset = 0
                        v.container.finishRefreshWithNoMoreData()
                        v.state.showEmpty()
                        v.container.setEnableLoadMore(false)
                    }
                    else {
                        if (current == TAB_LATEST) topicUpper = topics.last().tid
                        else topicOffset = topics.size
                        v.container.finishRefresh()
                        v.state.showContent()
                        v.container.setEnableLoadMore(true)
                    }
                    adapter.setSource(topics)
                    adapter.notifySource()
                }
                API.Code.UNAUTHORIZED, API.Code.FAILED -> {
                    v.state.showError(result.msg) { requestNewData() }
                    v.container.finishRefresh()
                }
                else -> {
                    v.state.showOffline { requestNewData() }
                    v.container.finishRefresh()
                }
            }
        }
    }

    @NewThread
    private fun requestNewDataMore() {
        lifecycleScope.launch {
            val current = v.tab.current
            val result = withContext(Dispatchers.IO) {
                if (current == TAB_LATEST) API.UserAPI.getLatestTopic(topicUpper)
                else API.UserAPI.getHotTopic(topicOffset)
            }
            if (result.success) {
                val topics = result.data
                if (topics.isEmpty()) v.container.finishLoadMoreWithNoMoreData()
                else {
                    val newCount = topics.size
                    if (current == TAB_LATEST) topicUpper = topics.last().tid
                    else topicOffset += topics.size
                    adapter.addSource(topics)
                    adapter.notifyItemRangeInserted(adapter.size - newCount, newCount)
                    v.container.finishLoadMore()
                }
            }
            else {
                tip(Tip.ERROR, result.msg)
                v.container.finishLoadMore()
            }
        }
    }
}