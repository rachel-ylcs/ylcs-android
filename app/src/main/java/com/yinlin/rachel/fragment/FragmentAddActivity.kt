package com.yinlin.rachel.fragment

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.haibin.calendarview.Calendar
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.activity.ShowActivity
import com.yinlin.rachel.databinding.FragmentAddActivityBinding
import com.yinlin.rachel.tool.date
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.view.ImageSelectView

class FragmentAddActivity(main: MainActivity, private val calendar: Calendar) : RachelFragment<FragmentAddActivityBinding>(main) {
    private val schemeAPPWebView = WebView(main)

    override fun bindingClass() = FragmentAddActivityBinding::class.java

    @SuppressLint("SetJavaScriptEnabled")
    override fun init() {
        v.pics.listener = object : ImageSelectView.Listener {
            override fun onOverflow(maxNum: Int) {
                tip(Tip.WARNING, "最多只能添加${maxNum}张图片")
            }

            override fun onImageClicked(position: Int, images: List<RachelPreview>) {
                main.navigate(FragmentImagePreview(main, images, position))
            }
        }

        v.fetchShowstart.rachelClick {
            val showstart = v.showstart.text
            if (showstart.isEmpty()) tip(Tip.WARNING, "还没有输入秀动ID")
            else if (showstart.startsWith("mlink")) tip(Tip.WARNING, "已获取到秀动链接")
            else schemeAPPWebView.loadUrl("https://wap.showstart.com/pages/activity/detail/detail?activityId=${showstart}")
        }

        schemeAPPWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        schemeAPPWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (view.progress == 100) {
                    if (url.contains("showstart")) {
                        val script = "document.getElementById('openApp').click();"
                        view.evaluateJavascript(script, null)
                    }
                }
                println(url)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                if (request.url.scheme == "mlink") {
                    v.showstart.text = request.url.toString()
                    tip(Tip.SUCCESS, "提取秀动链接成功")
                }
            }
        }

        v.ok.rachelClick {
            val title = v.title.text
            val content = v.content.text
            if (title.isEmpty() || content.isEmpty()) tip(Tip.WARNING, "活动名称或内容不能为空")
            else {
                main.sendMessage(RachelTab.me, RachelMessage.ME_ADD_ACTIVITY, calendar,
                    ShowActivity(calendar.date, title, content, v.pics.images,
                        v.showstart.text.ifEmpty { null }, v.damai.text.ifEmpty { null },
                        v.maoyan.text.ifEmpty { null })
                )
                main.pop()
            }
        }
    }

    override fun quit() {
        schemeAPPWebView.destroy()
    }

    override fun back(): BackState {
        if (v.title.text.isNotEmpty() || v.content.text.isNotEmpty() || v.pics.images.isNotEmpty()) {
            RachelDialog.confirm(main, "添加活动", "您确定要放弃已经填写的内容吗") { main.pop() }
            return BackState.CANCEL
        }
        return BackState.POP
    }
}