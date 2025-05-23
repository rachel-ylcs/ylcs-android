package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.annotation.HeaderLayout
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.topic.TopicPreview
import com.yinlin.rachel.databinding.FragmentProfileBinding
import com.yinlin.rachel.databinding.HeaderProfileBinding
import com.yinlin.rachel.databinding.ItemTopicBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelHeaderAdapter
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelImageLoader.loadDaily
import com.yinlin.rachel.model.RachelImageLoader.loadLoading
import com.yinlin.rachel.tool.pureColor
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.tool.visible

@Layout(FragmentProfileBinding::class)
class FragmentProfile(main: MainActivity, private val profileUid: Int) : RachelFragment<FragmentProfileBinding>(main) {
    @HeaderLayout(HeaderProfileBinding::class, ItemTopicBinding::class)
    class Adapter(private val fragment: FragmentProfile) : RachelHeaderAdapter<HeaderProfileBinding, ItemTopicBinding, TopicPreview>() {
        private val main = fragment.main

        override fun initHeader(v: HeaderProfileBinding) {
            requestUserProfile()
        }

        override fun update(v: ItemTopicBinding, item: TopicPreview, position: Int) {
            if (item.pic == null) v.pic.pureColor = 0
            else v.pic.loadLoading(item.picPath)
            v.top.visible = item.isTop
            v.title.text = item.title
            v.comment.text = item.commentNum.toString()
            v.coin.text = item.coinNum.toString()
        }

        override fun onItemClicked(v: ItemTopicBinding, item: TopicPreview, position: Int) {
            main.navigate(FragmentTopic(main, item.tid))
        }

        // 请求用户资料卡
        @IOThread
        private fun requestUserProfile() {
            val loading = main.loading
            fragment.startIOWithResult({ API.UserAPI.getProfile(fragment.profileUid) }) {
                loading.dismiss()
                if (it.success) {
                    val profile = it.data
                    header.apply {
                        name.text = profile.name
                        label.setLabel(profile.label, profile.level)
                        signature.text = profile.signature
                        level.text = profile.level.toString()
                        coin.text = profile.coin.toString()
                        val user = Config.user
                        if (user != null && fragment.profileUid == user.uid) {
                            avatar.load(profile.avatarPath, Config.cache_key_avatar)
                            wall.load(profile.wallPath, Config.cache_key_wall)
                        }
                        else {
                            avatar.loadDaily(profile.avatarPath)
                            wall.loadDaily(profile.wallPath)
                        }
                    }
                    setSource(profile.topics)
                    notifySourceEx()
                }
                else {
                    main.pop()
                    fragment.tip(Tip.ERROR, "用户不存在")
                }
            }
        }
    }

    private val adapter = Adapter(this)

    override fun init() {
        v.list.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
            setItemViewCacheSize(4)
            adapter = this@FragmentProfile.adapter
        }
    }

    override fun back() = BackState.POP

    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            RachelMessage.PROFILE_DELETE_TOPIC -> {
                val tid = args[0] as Int
                val index = adapter.findItem { it.tid == tid }
                if (index != -1) {
                    adapter.removeItem(index)
                    adapter.notifyRemovedEx(index)
                }
            }
            RachelMessage.PROFILE_UPDATE_TOPIC_TOP -> {
                val tid = args[0] as Int
                val isTop = args[1] as Boolean
                val index = adapter.findItem { it.tid == tid }
                if (index != -1) {
                    val comment = adapter[index]
                    if (comment.isTop != isTop) {
                        comment.isTop = isTop
                        adapter.notifyChangedEx(index)
                        if (index != 0) {
                            adapter.swapItem(index, 0)
                            adapter.notifyMovedEx(index, 0)
                        }
                    }
                }
            }
            else -> { }
        }
    }
}