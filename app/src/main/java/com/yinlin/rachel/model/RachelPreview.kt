package com.yinlin.rachel.model

class RachelPreview(var mImageUrl: String, var mSourceUrl: String = mImageUrl, var mVideoUrl: String = "") {
    companion object {
        fun fromSingleUri(items: Collection<*>) = items.map { RachelPreview(it.toString()) }
        inline fun <T> fromSingleUri(items: Collection<T>, fetch: (T) -> String) = items.map { RachelPreview(fetch(it)) }
    }

    val isImage: Boolean get() = mVideoUrl.isEmpty()
    val isVideo: Boolean get() = mVideoUrl.isNotEmpty()
}