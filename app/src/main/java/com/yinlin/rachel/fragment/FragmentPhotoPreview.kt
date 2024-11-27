package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Net
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.common.SimpleImageDownloadListener
import com.yinlin.rachel.databinding.FragmentPhotoPreviewBinding
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadBlack
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentPhotoPreview(main: MainActivity, private val pic: RachelPreview) : RachelFragment<FragmentPhotoPreviewBinding>(main) {
    override fun bindingClass() = FragmentPhotoPreviewBinding::class.java

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

    override fun back() = true

    @NewThread
    fun download4KRes() {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { API.UserAPI.download4KRes(Config.token) }
            loading.dismiss()
            if (result.success) downloadPicture(pic.mSourceUrl)
            else tip(Tip.ERROR, result.msg)
        }
    }

    @NewThread
    private fun downloadPicture(url: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Net.download(url, listener = SimpleImageDownloadListener(this@FragmentPhotoPreview))
            }
        }
    }
}