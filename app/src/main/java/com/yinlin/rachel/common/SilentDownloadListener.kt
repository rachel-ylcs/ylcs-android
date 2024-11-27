package com.yinlin.rachel.common

import com.yinlin.rachel.Net

abstract class SilentDownloadListener : Net.DownLoadListener {
    override suspend fun onStart() { }
    override suspend fun onSize(totalSize: Long) { }
    override suspend fun onDownloadTick(size: Long) { }
    override suspend fun onStop() { }
    override suspend fun isCancel() = false
    override suspend fun onCancel() { }
    override suspend fun onCompleted() { }
    override suspend fun onFailed() { }
}