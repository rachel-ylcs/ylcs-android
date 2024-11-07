package com.yinlin.rachel.data.res


class ResFolder(parent: ResFolder?, name: String, author: String)
    : ResFile(parent, name, author, null) {
    val items = mutableListOf<ResFile>()

    companion object {
        val emptyRes get() = ResFolder(null, "", "")
    }
}