package com.yinlin.rachel.page.common

import android.content.Intent
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.activity.VideoActivity
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.databinding.ItemWeiboBinding
import com.yinlin.rachel.fragment.FragmentImagePreview
import com.yinlin.rachel.fragment.FragmentWeibo
import com.yinlin.rachel.fragment.FragmentWeiboUser
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.rachelClick
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter

class WeiboAdapter(private val main: MainActivity) : RachelAdapter<ItemWeiboBinding, Weibo>() {
    private val rilNet = RachelImageLoader(main, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)

    override fun bindingClass() = ItemWeiboBinding::class.java

    override fun init(holder: RachelViewHolder<ItemWeiboBinding>, v: ItemWeiboBinding) {
        v.avatar.rachelClick {
            main.navigate(FragmentWeiboUser(main, this[holder.bindingAdapterPosition].user.userId))
        }
        v.text.setOnClickATagListener { _, _, _ -> true }
        v.pics.listener = Listener@ { position, _ ->
            val images = v.pics.images
            if (images.size == 1) {
                val image = images[0]
                if (image.isVideo) {
                    val intent = Intent(main, VideoActivity::class.java)
                    intent.putExtra("uri", image.mVideoUrl)
                    main.startActivity(intent)
                    return@Listener
                }
            }
            main.navigate(FragmentImagePreview(main, v.pics.images, position))
        }
        v.weiboCard.rachelClick {
            main.navigate(FragmentWeibo(main, this[holder.bindingAdapterPosition]))
        }
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