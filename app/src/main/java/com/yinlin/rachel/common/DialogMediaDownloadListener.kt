package com.yinlin.rachel.common

import android.content.Context
import com.yinlin.rachel.model.RachelDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class DialogMediaDownloadListener(context: Context) : MediaDownloadListener(context) {
    private var dialog: RachelDialog.Companion.DialogProgress? = null

    override suspend fun onStart() {
        withContext(Dispatchers.Main) { dialog = RachelDialog.progress(context, "下载中...") }
    }

    override suspend fun onSize(totalSize: Long) {
        withContext(Dispatchers.Main) { dialog?.maxProgress = totalSize.toInt() }
    }

    override suspend fun onDownloadTick(size: Long) {
        withContext(Dispatchers.Main) { dialog?.progress = size.toInt() }
    }

    override suspend fun isCancel() = dialog?.isCancel ?: false

    override suspend fun onStop() {
        withContext(Dispatchers.Main) { dialog?.dismiss() }
    }
}