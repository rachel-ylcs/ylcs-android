package com.yinlin.rachel.fragment

import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.lib.photoview.PhotoView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Net
import com.yinlin.rachel.Tip
import com.yinlin.rachel.databinding.FragmentImagePreviewBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadBlack
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.transformer.RotateYTransformer

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

    private val downLoadMediaListener = object : Net.DownLoadMediaListener {
        override fun onCancel() { }
        override fun onDownloadComplete(status: Boolean, uri: Uri?) {
            if (status) tip(Tip.SUCCESS, "下载成功")
            else tip(Tip.ERROR, "下载失败")
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

        v.downloadImage.rachelClick { Net.downloadPicture(main, pics[getFuckIndex(v.list.currentItem)].mImageUrl, downLoadMediaListener) }
        v.downloadSource.rachelClick { Net.downloadPicture(main, pics[getFuckIndex(v.list.currentItem)].mSourceUrl, downLoadMediaListener) }
        v.downloadAll.rachelClick { Net.downloadPictures(main, pics.filter { it.isImage }.map { it.mSourceUrl }, downLoadMediaListener) }
    }

    override fun back() = true

    // What Fuck Bug? When Can It Be Fixed?
    private fun setFuckIndex(index: Int) = index + 1

    // What Fuck Bug? When Can It Be Fixed?
    private fun getFuckIndex(index: Int) = if (v.list.realCount > 1) index - 1 else index
}