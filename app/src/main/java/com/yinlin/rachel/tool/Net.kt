package com.yinlin.rachel.tool

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.OutputStream
import java.net.Proxy
import java.util.concurrent.TimeUnit


object Net {
    private const val DOWNLOAD_BUFFER_SIZE = 1024 * 64

    private val client: OkHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val fileClient: OkHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(5, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    fun <R> get(url: String, headers: Map<String, String>? = null, block: (Response) -> R): R? = try {
        val builder = Request.Builder().url(url)
        headers?.let { builder.headers(it.toHeaders()) }
        client.newCall(builder.build()).execute().use(block)
    }
    catch (_: Exception) { null }

    fun get(url: String, headers: Map<String, String>? = null): JsonElement = try {
        val builder = Request.Builder().url(url)
        headers?.let { builder.headers(it.toHeaders()) }
        client.newCall(builder.build()).execute().use { it.body?.string().parseJson }
    } catch (_: Exception) { JsonNull.INSTANCE }

    fun post(url: String, data: JsonElement = JsonNull.INSTANCE, headers: Map<String, String>? = null): JsonElement = try {
        val builder = Request.Builder().url(url)
        headers?.let { builder.headers(it.toHeaders()) }
        builder.post(data.jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
        client.newCall(builder.build()).execute().use { it.body?.string().parseJson }
    }
    catch (_: Exception) { JsonNull.INSTANCE }

    fun postForm(url: String, files: Map<String, String>, content: Map<String, String>?, headers: Map<String, String>? = null): JsonElement = try {
        val builder = Request.Builder().url(url)
        headers?.let { builder.headers(it.toHeaders()) }
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        content?.forEach { (key, value) -> bodyBuilder.addFormDataPart(key, value) }
        files.forEach { (key, value) ->
            val file = File(value)
            if (file.exists()) bodyBuilder.addFormDataPart(key, file.name, file.asRequestBody("file/raw".toMediaTypeOrNull()))
        }
        builder.post(bodyBuilder.build())
        fileClient.newCall(builder.build()).execute().use { it.body?.string().parseJson }
    }
    catch (_: Exception) { JsonNull.INSTANCE }

    interface DownLoadListener {
        // 开始下载
        suspend fun onStart()
        // 准备输出流 url 是下载链接
        suspend fun onPrepare(url: String): OutputStream?
        // totalSize 是下载总字节数
        suspend fun onSize(totalSize: Long)
        // size 是已经下载的字节数, 若返回 true 则提前取消下载
        suspend fun onDownloadTick(size: Long)
        // 结束下载
        suspend fun onStop()
        // 是否取消
        suspend fun isCancel(): Boolean
        // 取消下载
        suspend fun onCancel()
        // 下载成功
        suspend fun onCompleted()
        // 下载失败
        suspend fun onFailed()
    }

    suspend fun download(url: String, headers: Map<String, String>? = null, listener: DownLoadListener): Boolean {
        var isSuccess = true
        var isCancel = false
        listener.onStart()
        try {
            val builder = Request.Builder().url(url)
            headers?.let { builder.headers(it.toHeaders()) }
            downloadClient.newCall(builder.build()).execute().use { response ->
                response.body!!.let { body ->
                    body.byteStream().use { inputStream ->
                        listener.onPrepare(url)!!.use { outputStream ->
                            listener.onSize(body.contentLength())
                            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                            var bytesRead: Int
                            var bytesReadTotal = 0L
                            while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                                if (listener.isCancel()) {
                                    isCancel = true
                                    break
                                }
                                outputStream.write(buffer, 0, bytesRead)
                                bytesReadTotal += bytesRead
                                listener.onDownloadTick(bytesReadTotal)
                            }
                            outputStream.flush()
                        }
                    }
                }
            }
        }
        catch (_: Exception) { isSuccess = false }
        listener.onStop()
        if (isCancel) listener.onCancel()
        else if (isSuccess) listener.onCompleted()
        else listener.onFailed()
        return !isCancel && isSuccess
    }

    suspend fun downloadAll(urls: List<String>, headers: Map<String, String>? = null, listener: DownLoadListener): Boolean {
        if (urls.isEmpty()) return false
        var isSuccess = true
        var isCancel = false
        listener.onStart()
        listener.onSize(urls.size.toLong())
        try {
            for ((currentIndex, url) in urls.withIndex()) {
                if (isCancel) break
                val builder = Request.Builder().url(url)
                headers?.let { builder.headers(it.toHeaders()) }
                downloadClient.newCall(builder.build()).execute().use { response ->
                    response.body!!.let { body ->
                        body.byteStream().use { inputStream ->
                            listener.onPrepare(url)!!.use { outputStream ->
                                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                                var bytesRead: Int
                                while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                                    if (listener.isCancel()) {
                                        isCancel = true
                                        break
                                    }
                                    outputStream.write(buffer, 0, bytesRead)
                                }
                                outputStream.flush()
                                listener.onDownloadTick(currentIndex + 1L)
                            }
                        }
                    }
                }
            }
        }
        catch (_: Exception) { isSuccess = false }
        listener.onStop()
        if (isCancel) listener.onCancel()
        else if (isSuccess) listener.onCompleted()
        else listener.onFailed()
        return !isCancel && isSuccess
    }
}