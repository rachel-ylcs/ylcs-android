package com.yinlin.rachel.fragment

import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.lib.photoview.PhotoView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Net
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.common.SimpleImageDownloadListener
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentImagePreviewBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadBlack
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.startIO
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.transformer.RotateYTransformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentImagePreview(main: MainActivity, private val pics: List<RachelPreview>, private val position: Int)
    : RachelFragment<FragmentImagePreviewBinding>(main) {

    class ViewHolder(pic: PhotoView) : RecyclerView.ViewHolder(pic)

    class Adapter(pics: List<RachelPreview>) : BannerAdapter<RachelPreview, ViewHolder>(pics) {
        override fun onCreateHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val pic = PhotoView(parent.context)
            pic.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return ViewHolder(pic)
        }

        override fun onBindView(holder: ViewHolder, item: RachelPreview, position: Int, size: Int) {
            val pic = holder.itemView as PhotoView
            pic.loadBlack(item.mImageUrl)
        }
    }

    constructor(main: MainActivity, pic: RachelPreview) : this(main, listOf(pic), 0)

    override fun bindingClass() = FragmentImagePreviewBinding::class.java

    override fun init() {
        v.list.apply {
            startPosition = setFuckIndex(position)
            indicator = CircleIndicator(context)
            setPageTransformer(RotateYTransformer())
            setAdapter(Adapter(pics), true)
        }

        v.downloadImage.rachelClick { downloadPicture(pics[getFuckIndex(v.list.currentItem)].mImageUrl) }
        v.downloadSource.rachelClick { downloadPicture(pics[getFuckIndex(v.list.currentItem)].mSourceUrl) }
        v.downloadAll.rachelClick { downloadPictures(pics.filter { it.isImage }.map { it.mSourceUrl }) }
    }

    override fun back() = BackState.POP

    // What Fuck Bug? When Can It Be Fixed?
    private fun setFuckIndex(index: Int) = index + 1

    // What Fuck Bug? When Can It Be Fixed?
    private fun getFuckIndex(index: Int) = if (v.list.realCount > 1) index - 1 else index

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