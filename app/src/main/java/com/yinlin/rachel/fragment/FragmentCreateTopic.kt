package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.topic.TopicPreview
import com.yinlin.rachel.data.user.User
import com.yinlin.rachel.databinding.FragmentCreateTopicBinding
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rachelClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentCreateTopic(main: MainActivity) : RachelFragment<FragmentCreateTopicBinding>(main) {
    override fun bindingClass() = FragmentCreateTopicBinding::class.java

    override fun init() {
        v.cancel.rachelClick { main.pop() }

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

    override fun back(): BackState {
        if (v.title.text.isNotEmpty() || v.content.text.isNotEmpty() || v.picList.isNotEmpty) {
            RachelDialog.confirm(main, "发表主题", "您确定要放弃已经填写的内容吗") { main.pop() }
            return BackState.CANCEL
        }
        return BackState.POP
    }

    @NewThread
    private fun sendTopic(user: User, title: String, content: String, pics: List<String>) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.sendTopic(Config.token, title, content, pics) }
            loading.dismiss()
            if (result.success) {
                val topPreview = TopicPreview(result.data.tid, user.uid, user.name, title, result.data.pic, false, 0, 0)
                main.sendMessage(RachelTab.discovery, RachelMessage.DISCOVERY_ADD_TOPIC, topPreview)
                main.pop()
                tip(Tip.SUCCESS, result.msg)
            }
            else tip(Tip.ERROR, result.msg)
        }
    }
}