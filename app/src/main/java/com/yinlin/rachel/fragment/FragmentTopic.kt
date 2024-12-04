package com.yinlin.rachel.fragment

import android.text.InputType
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.compareLatestTime
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.topic.Comment
import com.yinlin.rachel.data.topic.Topic
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentTopicBinding
import com.yinlin.rachel.databinding.HeaderTopicBinding
import com.yinlin.rachel.databinding.ItemCommentBinding
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelHeaderAdapter
import com.yinlin.rachel.model.RachelImageLoader.loadDaily
import com.yinlin.rachel.model.RachelPopMenu
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FragmentTopic(main: MainActivity, private val tid: Int) : RachelFragment<FragmentTopicBinding>(main) {
    class Adapter(private val fragment: FragmentTopic) : RachelHeaderAdapter<HeaderTopicBinding, ItemCommentBinding, Comment>() {
        private val main = fragment.main

        override fun bindingHeaderClass() = HeaderTopicBinding::class.java
        override fun bindingItemClass() = ItemCommentBinding::class.java

        override fun initHeader(v: HeaderTopicBinding) {
            v.avatar.rachelClick { main.navigate(FragmentProfile(main, fragment.topic.uid)) }
            v.pics.listener = { position, _ -> main.navigate(FragmentImagePreview(main, v.pics.images, position)) }

            // 菜单
            v.more.rachelClick(300) {
                val menuList = ArrayList<RachelPopMenu.Item>()
                val user = Config.loginUser
                if (user != null) {
                    val topicId = fragment.topic.uid
                    val topText = if (fragment.topic.isTop) "取消置顶" else "置顶"
                    if (user.canUpdateTopicTop(topicId)) menuList += RachelPopMenu.Item(topText) {
                        RachelDialog.confirm(main, content="${topText}此主题?") {
                            fragment.updateTopicTop(!fragment.topic.isTop)
                        }
                    }
                    if (user.canDeleteTopic(topicId)) menuList += RachelPopMenu.Item("删除") {
                        RachelDialog.confirm(main, content="删除此主题?") {
                            fragment.deleteTopic()
                        }
                    }
                    if (menuList.isNotEmpty()) RachelPopMenu.showDown(it, menuList)
                }
            }

            requestTopic()
        }

        override fun init(holder: RachelItemViewHolder<ItemCommentBinding>, v: ItemCommentBinding) {
            v.avatar.rachelClick { main.navigate(FragmentProfile(main, this[holder.positionEx].uid)) }
            v.more.rachelClick(300) {
                val menuList = ArrayList<RachelPopMenu.Item>()
                val user = Config.loginUser
                val topicUid = fragment.topic.uid
                if (user != null) {
                    val position = holder.positionEx
                    val comment = this[position]
                    val topText = if (comment.isTop) "取消置顶" else "置顶"
                    if (user.canUpdateCommentTop(topicUid)) menuList += RachelPopMenu.Item(topText) {
                        RachelDialog.confirm(main, content="${topText}此评论?") {
                            fragment.updateCommentTop(comment.cid, position, !comment.isTop)
                        }
                    }
                    if (user.canDeleteComment(topicUid, comment.uid)) menuList += RachelPopMenu.Item("删除") {
                        RachelDialog.confirm(main, content="删除此评论?") {
                            fragment.deleteComment(comment.cid, position)
                        }
                    }
                    if (menuList.isNotEmpty()) RachelPopMenu.showDown(it, menuList)
                }
            }
        }

        override fun update(v: ItemCommentBinding, item: Comment, position: Int) {
            v.name.text = item.name
            v.time.text = item.ts
            v.avatar.loadDaily(item.avatarPath)
            v.content.text = item.content
            v.userLabel.setLabel(item.label, item.level)
            v.top.visible = item.isTop
        }

        @IOThread
        fun requestTopic() {
            fragment.lifecycleScope.launch {
                val loading = main.loading
                val result = withContext(Dispatchers.IO) { API.UserAPI.getTopic(fragment.tid) }
                loading.dismiss()
                if (result.success) {
                    val topic = result.data
                    fragment.topic = topic
                    header.apply {
                        name.text = topic.name
                        time.text = topic.ts
                        avatar.loadDaily(topic.avatarPath)
                        title.text = topic.title
                        content.text = topic.content
                        userLabel.setLabel(topic.label, topic.level)
                        pics.images = RachelPreview.fromSingleUri(topic.pics) { topic.picPath(it) }
                    }
                    setSource(topic.comments)
                    notifySourceEx()
                }
                else {
                    main.pop()
                    fragment.tip(Tip.ERROR, "主题不存在")
                }
            }
        }
    }

    private lateinit var topic: Topic
    private val adapter = Adapter(this)

    override fun bindingClass() = FragmentTopicBinding::class.java

    override fun init() {
        v.list.apply {
            layoutManager = LinearLayoutManager(main)
            recycledViewPool.setMaxRecycledViews(0, 16)
            setItemViewCacheSize(6)
            adapter = this@FragmentTopic.adapter
        }

        v.frontCard.rachelClick { openCommentCard() }

        // 评论
        v.buttonSend.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeTopic) {
                    val content = v.comment.text
                    if (content.isEmpty()) tip(Tip.WARNING, "你还没有输入任何内容呢")
                    else sendComment(user, content)
                }
                else tip(Tip.WARNING, "你没有权限")
            }
            else tip(Tip.WARNING, "请先登录")
        }

        // 投币
        v.buttonCoin.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeTopic) {
                    if (topic.uid == user.uid) tip(Tip.WARNING, "不能给自己投币哦")
                    else RachelDialog.input(main, "请输入投币数量(1-3)", 1, inputType=InputType.TYPE_CLASS_NUMBER) {
                        val value = it.toIntOrNull() ?: 0
                        if (user.coin < value) tip(Tip.WARNING, "你的银币不够哦")
                        else if (value in 1..3) sendCoin(value)
                        else tip(Tip.WARNING, "不能投${value}个币哦")
                    }
                }
                else tip(Tip.WARNING, "你没有权限")
            }
            else tip(Tip.WARNING, "请先登录")
        }
    }

    override fun back(): BackState {
        if (v.commentCard.visible) {
            closeCommentCard()
            return BackState.CANCEL
        }
        return BackState.POP
    }

    private fun openCommentCard() {
        v.comment.requestFocus()
        v.commentCard.visible = true
    }

    private fun closeCommentCard() {
        v.comment.text = ""
        v.commentCard.visible = false
    }

    @IOThread
    private fun sendComment(user: User, content: String) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.sendComment(Config.token, tid, content) }
            loading.dismiss()
            if (result.success) {
                val comment = result.data
                closeCommentCard()
                adapter += Comment(comment.cid, user.uid, user.name, comment.ts, content, false, user.label, user.coin)
                adapter.notifyInsertEx(adapter.size)
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    @IOThread
    private fun sendCoin(value: Int) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.sendCoin(Config.token, topic.uid, tid, value) }
            loading.dismiss()
            if (result.success) {
                main.sendMessage(RachelTab.me, RachelMessage.ME_REQUEST_USER_INFO)
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    @IOThread
    private fun deleteComment(cid: Long, position: Int) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.deleteComment(Config.token, cid, tid) }
            loading.dismiss()
            if (result.success) {
                adapter.removeItem(position)
                adapter.notifyRemovedEx(position)
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    @IOThread
    private fun updateCommentTop(cid: Long, position: Int, isTop: Boolean) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.updateCommentTop(Config.token, cid, tid, isTop) }
            loading.dismiss()
            if (result.success) {
                val comment = adapter[position]
                if (comment.isTop != isTop) {
                    comment.isTop = isTop
                    val items = adapter.items
                    val activeComment = items[position]
                    var targetIndex = if (isTop) position else items.size - 1
                    if (isTop) {
                        // 只需要找 0 到 position-1 的评论
                        for (index in 0..< position) {
                            val currentComment = items[index]
                            // 如果此评论比置顶评论更晚或不是置顶评论则跳出
                            if (compareLatestTime(currentComment.ts, activeComment.ts) || !currentComment.isTop) {
                                targetIndex = index
                                break
                            }
                        }
                    }
                    else {
                        // 只需要找 position+1 到 size-1 的评论
                        for (index in position + 1..< items.size) {
                            val currentComment = items[index]
                            if (currentComment.isTop) continue
                            if (compareLatestTime(currentComment.ts, activeComment.ts)) {
                                targetIndex = index - 1
                                break
                            }
                        }
                    }
                    adapter.notifyChangedEx(position)
                    if (position != targetIndex) {
                        adapter.moveItem(position, targetIndex)
                        adapter.notifyMovedEx(position, targetIndex)
                    }
                }
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    @IOThread
    private fun deleteTopic() {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.deleteTopic(Config.token, tid) }
            loading.dismiss()
            if (result.success) {
                main.sendMessage(RachelTab.discovery, RachelMessage.DISCOVERY_DELETE_TOPIC, tid)
                main.popMessage(FragmentProfile::class, RachelMessage.PROFILE_DELETE_TOPIC, tid)
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    @IOThread
    private fun updateTopicTop(isTop: Boolean) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.updateTopicTop(Config.token, tid, isTop) }
            loading.dismiss()
            if (result.success) {
                main.sendBottomMessage(FragmentProfile::class, RachelMessage.PROFILE_UPDATE_TOPIC_TOP, tid, isTop)
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }
}