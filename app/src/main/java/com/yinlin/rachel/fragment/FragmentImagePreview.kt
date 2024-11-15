package com.yinlin.rachel.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.luck.picture.lib.photoview.PhotoView
import com.yinlin.rachel.Net
import com.yinlin.rachel.Tip
import com.yinlin.rachel.databinding.FragmentImagePreviewBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.tip
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.transformer.RotateYTransformer

class FragmentImagePreview(pages: RachelPages, private val pics: List<RachelPreview>, private val position: Int)
    : RachelFragment<FragmentImagePreviewBinding>(pages) {

    class ViewHolder(pic: PhotoView) : RecyclerView.ViewHolder(pic)

    class Adapter(context: Context, pics: List<RachelPreview>) : BannerAdapter<RachelPreview, ViewHolder>(pics) {
        private val ril = RachelImageLoader(context, RequestOptions()
            .placeholder(ColorDrawable(Color.BLACK))
            .diskCacheStrategy(DiskCacheStrategy.ALL))

        override fun onCreateHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val pic = PhotoView(parent.context)
            pic.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return ViewHolder(pic)
        }

        override fun onBindView(holder: ViewHolder, item: RachelPreview, position: Int, size: Int) {
            val pic = holder.itemView as PhotoView
            pic.load(ril, item.mImageUrl)
        }
    }

    private val downLoadMediaListener = object : Net.DownLoadMediaListener {
        override fun onCancel() { }
        override fun onDownloadComplete(status: Boolean, uri: Uri?) {
            if (status) tip(Tip.SUCCESS, "下载成功")
            else tip(Tip.ERROR, "下载失败")
        }
    }

    constructor(pages: RachelPages, pic: RachelPreview) : this(pages, listOf(pic), 0)

    override fun bindingClass() = FragmentImagePreviewBinding::class.java

    override fun init() {
        v.list.apply {
            startPosition = setFuckIndex(position)
            indicator = CircleIndicator(context)
            setPageTransformer(RotateYTransformer())
            setAdapter(Adapter(pages.context, pics), true)
        }

        v.downloadImage.rachelClick { Net.downloadPicture(pages.context, pics[getFuckIndex(v.list.currentItem)].mImageUrl, downLoadMediaListener) }
        v.downloadSource.rachelClick { Net.downloadPicture(pages.context, pics[getFuckIndex(v.list.currentItem)].mSourceUrl, downLoadMediaListener) }
        v.downloadAll.rachelClick { Net.downloadPictures(pages.context, pics.filter { it.isImage }.map { it.mSourceUrl }, downLoadMediaListener) }
    }

    override fun back() = true

    // What Fuck Bug? When Can It Be Fixed?
    private fun setFuckIndex(index: Int) = index + 1

    // What Fuck Bug? When Can It Be Fixed?
    private fun getFuckIndex(index: Int) = if (v.list.realCount > 1) index - 1 else index
}