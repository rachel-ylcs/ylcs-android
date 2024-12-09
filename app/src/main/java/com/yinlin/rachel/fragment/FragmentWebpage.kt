package com.yinlin.rachel.fragment

import android.annotation.SuppressLint
import android.webkit.WebViewClient
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentWebpageBinding
import com.yinlin.rachel.model.RachelFragment

@Layout(FragmentWebpageBinding::class)
class FragmentWebpage(main: MainActivity, private val url: String) : RachelFragment<FragmentWebpageBinding>(main) {
    @SuppressLint("SetJavaScriptEnabled")
    override fun init() {
        v.web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        v.web.webViewClient = WebViewClient()
        v.web.loadUrl(url)
    }

    override fun quit() {
        v.web.destroy()
    }

    override fun back(): BackState {
        if (v.web.canGoBack()) {
            v.web.goBack()
            return BackState.CANCEL
        }
        return BackState.POP
    }
}