package com.yinlin.rachel.data.res

open class ResFile(val parent: ResFolder?, val name: String, val author: String, url: String?) {
    var thumbUrl: String? = null
    var sourceUrl: String? = null
    init {
        url?.let {
            val dotIndex = it.lastIndexOf('.')
            thumbUrl = if (dotIndex != -1) "https://img.picgo.net/${it.substring(0, dotIndex)}.md${it.substring(dotIndex)}" else "https://img.picgo.net/${this}.md"
            sourceUrl = "https://img.picgo.net/${it}"
        }
    }
}