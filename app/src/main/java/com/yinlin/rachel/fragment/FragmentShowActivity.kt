package com.yinlin.rachel.fragment

import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.activity.ShowActivity
import com.yinlin.rachel.databinding.FragmentShowActivityBinding
import com.yinlin.rachel.gotoAPP
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip

class FragmentShowActivity(pages: RachelPages, private val show: ShowActivity) : RachelFragment<FragmentShowActivityBinding>(pages) {
    private val showstartUrl: String? = show.showstart
    private val damaiUrl: String? = show.damai
    private val maoyanUrl: String? = show.maoyan

    override fun bindingClass() = FragmentShowActivityBinding::class.java

    override fun init() {
        v.showstart.rachelClick {
            if (showstartUrl != null) gotoAPP(pages.context, "秀动", showstartUrl)
            else tip(Tip.WARNING, "此平台未售票，请稍后再试")
        }
        v.damai.rachelClick {
            if (damaiUrl != null) pages.navigate(FragmentWebpage(pages, "https://m.damai.cn/shows/item.html?itemId=${damaiUrl}"))
            else tip(Tip.WARNING, "此平台未售票，请稍后再试")
        }
        v.maoyan.rachelClick {
            if (maoyanUrl != null) pages.navigate(FragmentWebpage(pages, "https://show.maoyan.com/qqw#/detail/${maoyanUrl}"))
            else tip(Tip.WARNING, "此平台未售票，请稍后再试")
        }

        v.title.text = show.title
        v.content.text = show.content
        v.pics.images = RachelPreview.fromSingleUri(show.pics) { show.picPath(it) }
        v.pics.listener = { pos, _ -> pages.navigate(FragmentImagePreview(pages, v.pics.images, pos)) }
    }

    override fun back() = true
}