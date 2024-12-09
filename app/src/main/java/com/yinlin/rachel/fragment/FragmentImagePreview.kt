package com.yinlin.rachel.fragment


import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Net
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.common.SimpleImageDownloadListener
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentImagePreviewBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.startIO

@Layout(FragmentImagePreviewBinding::class)
class FragmentImagePreview(main: MainActivity, private val pics: List<RachelPreview>, private val position: Int)
    : RachelFragment<FragmentImagePreviewBinding>(main) {
    constructor(main: MainActivity, pic: RachelPreview) : this(main, listOf(pic), 0)

    override fun init() {
        v.banner.setImages(pics, position)

        v.downloadImage.rachelClick { v.banner.current?.let { downloadPicture(it.mImageUrl) } }
        v.downloadSource.rachelClick { v.banner.current?.let { downloadPicture(it.mSourceUrl) } }
        v.downloadAll.rachelClick { downloadPictures(v.banner.images.filter { it.isImage }.map { it.mSourceUrl }) }
    }

    override fun back() = BackState.POP

    @IOThread
    private fun downloadPicture(url: String) {
        startIO {
            Net.download(url, listener = SimpleImageDownloadListener(this@FragmentImagePreview))
        }
    }

    @IOThread
    private fun downloadPictures(urls: List<String>) {
        startIO {
            Net.downloadAll(urls, listener = SimpleImageDownloadListener(this@FragmentImagePreview))
        }
    }
}