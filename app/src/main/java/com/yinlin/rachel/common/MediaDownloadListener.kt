package com.yinlin.rachel.common

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.yinlin.rachel.Net.DownLoadListener
import java.io.OutputStream

abstract class MediaDownloadListener(protected val context: Context) : DownLoadListener {
    protected var uri: Uri? = null

    abstract fun makeMediaUri(url: String, values: ContentValues): Uri

    override suspend fun onPrepare(url: String): OutputStream? {
        val values = ContentValues()
        val mediaUri = makeMediaUri(url, values)
        return context.contentResolver.insert(mediaUri, values)?.let {
            uri = it
            context.contentResolver.openOutputStream(it)
        }
    }

    override suspend fun onCancel() {
        uri?.let { context.contentResolver.delete(it, null, null) }
    }
}