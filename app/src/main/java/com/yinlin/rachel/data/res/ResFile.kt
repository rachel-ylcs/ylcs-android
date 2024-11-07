package com.yinlin.rachel.data.res

open class ResFile(val parent: ResFolder?, val name: String, val author: String, url: String?) {
    var thumbUrl: String? = null
    var sourceUrl: String? = null
    init {
        url?.apply {
            val dotIndex = this.lastIndexOf('.')
            thumbUrl = if (dotIndex != -1) "https://img.picgo.net/${this.substring(0, dotIndex)}.md${this.substring(dotIndex)}" else "https://img.picgo.net/${this}.md"
            sourceUrl = "https://img.picgo.net/${this}"
        }
    }
}