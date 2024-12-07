package com.yinlin.rachel.common

import android.content.Context
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.tool.withMain

abstract class DialogMediaDownloadListener(context: Context) : MediaDownloadListener(context) {
    private var dialog: RachelDialog.Companion.DialogProgress? = null

    override suspend fun onStart() {
        withMain { dialog = RachelDialog.progress(context, "下载中...") }
    }

    override suspend fun onSize(totalSize: Long) {
        withMain { dialog?.maxProgress = totalSize.toInt() }
    }

    override suspend fun onDownloadTick(size: Long) {
        withMain { dialog?.progress = size.toInt() }
    }

    override suspend fun isCancel() = dialog?.isCancel ?: false

    override suspend fun onStop() {
        withMain { dialog?.dismiss() }
    }
}