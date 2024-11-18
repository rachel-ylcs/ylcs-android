package com.yinlin.rachel.fragment

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.yinlin.rachel.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Net
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.databinding.FragmentPhotoPreviewBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentPhotoPreview(main: MainActivity, private val pic: RachelPreview) : RachelFragment<FragmentPhotoPreviewBinding>(main) {
    override fun bindingClass() = FragmentPhotoPreviewBinding::class.java

    private val ril = RachelImageLoader(main, RequestOptions()
        .placeholder(ColorDrawable(main.getColor(R.color.black)))
        .diskCacheStrategy(DiskCacheStrategy.ALL))

    private val simpleDownLoadMediaListener = object : Net.DownLoadMediaListener {
        override fun onCancel() { }
        override fun onDownloadComplete(status: Boolean, uri: Uri?) {
            if (status) tip(Tip.SUCCESS, "下载成功")
            else tip(Tip.ERROR, "下载失败")
        }
    }

    override fun init() {
        v.pic.load(ril, pic.mImageUrl)

        v.downloadHd.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.hasPrivilegeRes) Net.downloadPicture(main, pic.mImageUrl, simpleDownLoadMediaListener)
                else tip(Tip.WARNING, "你没有权限")
            }
            else tip(Tip.WARNING, "请先登录")
        }

        v.download4k.rachelClick {
            val user = Config.loginUser
            if (user != null) {
                if (user.canDownload4KRes) Net.downloadPicture(main, pic.mSourceUrl, simpleDownLoadMediaListener)
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
            if (result.success) Net.downloadPicture(main, pic.mSourceUrl, simpleDownLoadMediaListener)
            else tip(Tip.ERROR, result.msg)
        }
    }
}