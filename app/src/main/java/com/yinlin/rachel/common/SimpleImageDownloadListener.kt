package com.yinlin.rachel.common

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.model.RachelFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SimpleImageDownloadListener(private val fragment: RachelFragment<*>): DialogMediaDownloadListener(fragment.main) {
    override fun makeMediaUri(url: String, values: ContentValues): Uri {
        values.put(MediaStore.Images.Media.DISPLAY_NAME, url.substringAfterLast('/'))
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    override suspend fun onCompleted() {
        withContext(Dispatchers.Main) { fragment.tip(Tip.SUCCESS, "下载成功") }
    }

    override suspend fun onFailed() {
        withContext(Dispatchers.Main) { fragment.tip(Tip.ERROR, "下载失败") }
    }
}