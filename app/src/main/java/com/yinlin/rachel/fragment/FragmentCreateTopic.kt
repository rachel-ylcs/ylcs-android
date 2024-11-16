package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.Config
import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.topic.TopicPreview
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentCreateTopicBinding
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentCreateTopic(pages: RachelPages) : RachelFragment<FragmentCreateTopicBinding>(pages) {
    override fun bindingClass() = FragmentCreateTopicBinding::class.java

    override fun init() {
        v.cancel.rachelClick { pages.pop() }

        v.send.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                val title = v.title.text
                val content = v.content.text
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    if (user.hasPrivilegeTopic) sendTopic(user, title, content, v.picList.images)
                    else tip(Tip.WARNING, "你没有权限")
                }
                else tip(Tip.WARNING, "你还未填写任何内容呢")
            }
            else tip(Tip.WARNING, "请先登录")
        }
    }

    override fun back() = true

    @NewThread
    private fun sendTopic(user: User, title: String, content: String, pics: List<String>) {
        lifecycleScope.launch {
            val loading = RachelDialog.loading(pages.context)
            val result = withContext(Dispatchers.IO) { API.UserAPI.sendTopic(Config.token, title, content, pics) }
            loading.dismiss()
            if (result.success) {
                val topPreview = TopicPreview(result.data.tid, user.uid, user.name, title, result.data.pic, false, 0, 0)
                pages.sendMessage(RachelTab.discovery, RachelMessage.DISCOVERY_ADD_TOPIC, topPreview)
                pages.pop()
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }
}