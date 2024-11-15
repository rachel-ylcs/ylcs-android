package com.yinlin.rachel.page.common

import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.R
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.databinding.ItemWeiboBinding
import com.yinlin.rachel.fragment.FragmentImagePreview
import com.yinlin.rachel.fragment.FragmentWeibo
import com.yinlin.rachel.fragment.FragmentWeiboUser
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelOnClickListener
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.rachelClick
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter

class WeiboAdapter(private val pages: RachelPages) : RachelAdapter<ItemWeiboBinding, Weibo>() {
    private val rilNet = RachelImageLoader(pages.context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

    override fun bindingClass() = ItemWeiboBinding::class.java

    override fun init(holder: RachelViewHolder<ItemWeiboBinding>, v: ItemWeiboBinding) {
        v.avatar.rachelClick {
            pages.navigate(FragmentWeiboUser(pages, this[holder.bindingAdapterPosition].user.userId))
        }
        v.text.setOnClickATagListener { _, _, _ -> true }
        v.pics.listener = { position, _ -> pages.navigate(FragmentImagePreview(pages, v.pics.images, position)) }
        val detailsListener = RachelOnClickListener {
            pages.navigate(FragmentWeibo(pages, this[holder.bindingAdapterPosition]))
        }
        v.weiboCard.rachelClick(detailsListener)
    }

    override fun update(v: ItemWeiboBinding, item: Weibo, position: Int) {
        v.name.text = item.user.name
        v.avatar.load(rilNet, item.user.avatar)
        v.time.text = item.time
        v.location.text = item.user.location
        v.text.setHtml(item.text, HtmlHttpImageGetter(v.text))
        v.pics.images = item.pictures
        v.like.text = item.likeNum.toString()
        v.comment.text = item.commentNum.toString()
        v.repost.text = item.repostNum.toString()
    }
}