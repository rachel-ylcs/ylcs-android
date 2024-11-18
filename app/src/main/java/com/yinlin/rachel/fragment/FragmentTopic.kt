package com.yinlin.rachel.fragment

import android.text.InputType
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.topic.Comment
import com.yinlin.rachel.data.topic.Topic
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentTopicBinding
import com.yinlin.rachel.databinding.HeaderTopicBinding
import com.yinlin.rachel.databinding.ItemCommentBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelHeaderAdapter
import com.yinlin.rachel.model.RachelPopMenu
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import com.yinlin.rachel.visible
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
            v.avatar.load(main.ril, item.avatarPath)
            v.content.text = item.content
            v.userLabel.setLabel(item.label, item.level)
            v.top.visible = item.isTop
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

        requestTopic(tid)
    }

    override fun back(): Boolean {
        if (v.commentCard.visible) {
            closeCommentCard()
            return false
        }
        return true
    }

    private fun openCommentCard() {
        v.comment.requestFocus()
        v.commentCard.visible = true
    }

    private fun closeCommentCard() {
        v.comment.text = ""
        v.commentCard.visible = false
    }

    @NewThread
    fun requestTopic(topicId: Int) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.getTopic(topicId) }
            loading.dismiss()
            if (result.success) {
                topic = result.data
                adapter.header.apply {
                    name.text = topic.name
                    time.text = topic.ts
                    avatar.load(main.ril, topic.avatarPath)
                    title.text = topic.title
                    content.text = topic.content
                    userLabel.setLabel(topic.label, topic.level)
                    pics.images = RachelPreview.fromSingleUri(topic.pics) { topic.picPath(it) }
                }
                adapter.setSource(topic.comments)
                adapter.notifySourceEx()
            }
            else {
                main.pop()
                tip(Tip.ERROR, "主题不存在")
            }
        }
    }

    @NewThread
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

    @NewThread
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

    @NewThread
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

    @NewThread
    private fun updateCommentTop(cid: Long, position: Int, isTop: Boolean) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.updateCommentTop(Config.token, cid, tid, isTop) }
            loading.dismiss()
            if (result.success) {
                val comment = adapter[position]
                if (comment.isTop != isTop) {
                    comment.isTop = isTop
                    adapter.notifyChangedEx(position)
                    if (position != 0) {
                        adapter.swapItem(position, 0)
                        adapter.notifyMovedEx(position, 0)
                    }
                }
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }

    @NewThread
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

    @NewThread
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