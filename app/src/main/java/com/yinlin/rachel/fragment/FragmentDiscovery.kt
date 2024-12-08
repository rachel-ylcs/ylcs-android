package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.topic.TopicPreview
import com.yinlin.rachel.databinding.FragmentDiscoveryBinding
import com.yinlin.rachel.databinding.ItemTopicUserBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadDaily
import com.yinlin.rachel.model.RachelImageLoader.loadLoading
import com.yinlin.rachel.tool.isTop
import com.yinlin.rachel.tool.pureColor
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.view.NavigationView

class FragmentDiscovery(main: MainActivity) : RachelFragment<FragmentDiscoveryBinding>(main)  {
    class Adapter(fragment: FragmentDiscovery) : RachelAdapter<ItemTopicUserBinding, TopicPreview>() {
        private val main = fragment.main

        override fun bindingClass() = ItemTopicUserBinding::class.java

        override fun init(holder: RachelViewHolder<ItemTopicUserBinding>, v: ItemTopicUserBinding) {
            v.avatar.rachelClick {
                val position = holder.bindingAdapterPosition
                main.navigate(FragmentProfile(main, this[position].uid))
            }
        }

        override fun update(v: ItemTopicUserBinding, item: TopicPreview, position: Int) {
            if (item.pic == null) v.pic.pureColor = 0
            else v.pic.loadLoading(item.picPath)
            v.title.text = item.title
            v.name.text = item.name
            v.avatar.loadDaily(item.avatarPath)
            v.comment.text = item.commentNum.toString()
            v.coin.text = item.coinNum.toString()
        }

        override fun onItemClicked(v: ItemTopicUserBinding, item: TopicPreview, position: Int) {
            main.navigate(FragmentTopic(main, item.tid))
        }
    }

    companion object {
        const val TAB_LATEST = 0
        const val TAB_HOT = 1
    }

    private var topicUpper: Int = 2147483647
    private var topicOffset: Int = 0

    private val mAdapter = Adapter(this)

    override fun bindingClass() = FragmentDiscoveryBinding::class.java

    override fun init() {
        v.tab.simpleItems = listOf("最新", "热门")
        // 加载时不需要刷新数据 直到 start 调用
        v.tab.listener = object : NavigationView.Listener {
            override fun onSelected(position: Int, title: String, obj: Any?) {
                when (position) {
                    TAB_LATEST, TAB_HOT -> requestNewData()
                    else -> { }
                }
            }
        }

        v.buttonAdd.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeTopic) main.navigate(FragmentCreateTopic(main))
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
            adapter = mAdapter
        }
    }

    override fun start() {
        requestNewData()
    }

    override fun back(): BackState {
        if (v.list.isTop) return BackState.HOME
        else {
            v.list.smoothScrollToPosition(0)
            return BackState.CANCEL
        }
    }

    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            RachelMessage.DISCOVERY_ADD_TOPIC -> {
                // 只有在最新状态下更新, 热门无需更新
                if (v.tab.current == TAB_LATEST) {
                    mAdapter.addItem(0, args[0] as TopicPreview)
                    mAdapter.notifyItemInserted(0)
                    v.list.scrollToPosition(0)
                }
            }
            RachelMessage.DISCOVERY_DELETE_TOPIC -> {
                val tid = args[0] as Int
                val index = mAdapter.findItem { it.tid == tid }
                if (index != -1) {
                    mAdapter.removeItem(index)
                    mAdapter.notifyItemRemoved(index)
                }
            }
            else -> { }
        }
    }

    @IOThread
    private fun requestNewData() {
        v.container.setNoMoreData(false)
        v.list.scrollToPosition(0)
        v.state.showLoading()
        val current = v.tab.current
        startIOWithResult({
            if (current == TAB_LATEST) API.UserAPI.getLatestTopic()
            else API.UserAPI.getHotTopic()
        }) {
            when (it.code) {
                API.Code.SUCCESS -> {
                    val topics = it.data
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
                    mAdapter.setSource(topics)
                    mAdapter.notifySource()
                }
                API.Code.UNAUTHORIZED, API.Code.FAILED -> {
                    v.state.showError { requestNewData() }
                    v.container.finishRefresh()
                }
                else -> {
                    v.state.showOffline { requestNewData() }
                    v.container.finishRefresh()
                }
            }
        }
    }

    @IOThread
    private fun requestNewDataMore() {
        val current = v.tab.current
        startIOWithResult({
            if (current == TAB_LATEST) API.UserAPI.getLatestTopic(topicUpper)
            else API.UserAPI.getHotTopic(topicOffset)
        }) {
            if (it.success) {
                val topics = it.data
                if (topics.isEmpty()) v.container.finishLoadMoreWithNoMoreData()
                else {
                    val newCount = topics.size
                    if (current == TAB_LATEST) topicUpper = topics.last().tid
                    else topicOffset += topics.size
                    mAdapter.addSource(topics)
                    mAdapter.notifyItemRangeInserted(mAdapter.size - newCount, newCount)
                    v.container.finishLoadMore()
                }
            }
            else {
                tip(Tip.ERROR, it.msg)
                v.container.finishLoadMore()
            }
        }
    }
}