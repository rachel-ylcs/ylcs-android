package com.yinlin.rachel.fragment

import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Net
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.api.API
import com.yinlin.rachel.common.SimpleImageDownloadListener
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentPhotoPreviewBinding
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadBlack
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.startIO
import com.yinlin.rachel.tool.startIOWithResult

@Layout(FragmentPhotoPreviewBinding::class)
class FragmentPhotoPreview(main: MainActivity, private val pic: RachelPreview) : RachelFragment<FragmentPhotoPreviewBinding>(main) {
    override fun init() {
        v.pic.loadBlack(pic.mImageUrl)

        v.downloadHd.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeRes) downloadPicture(pic.mImageUrl)
                else tip(Tip.WARNING, "你没有权限")
            }
            else tip(Tip.WARNING, "请先登录")
        }

        v.download4k.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.canDownload4KRes) downloadPicture(pic.mSourceUrl)
                else {
                    RachelDialog.confirm(main, content="未达到8级解锁4K下载权限, 是否使用小银币*1临时下载") {
                        if (user.coin < 1) tip(Tip.WARNING, "你的银币不够哦")
                        else download4KRes()
                    }
                }
            }
            else tip(Tip.WARNING, "请先登录")
        }
    }

    override fun back() = BackState.POP

    @IOThread
    fun download4KRes() {
        val loading = main.loading
        startIOWithResult({ API.UserAPI.download4KRes(Config.token) }) {
            loading.dismiss()
            if (it.success) downloadPicture(pic.mSourceUrl)
            else tip(Tip.ERROR, it.msg)
        }
    }

    @IOThread
    private fun downloadPicture(url: String) {
        startIO { Net.download(url, listener = SimpleImageDownloadListener(this@FragmentPhotoPreview)) }
    }
}