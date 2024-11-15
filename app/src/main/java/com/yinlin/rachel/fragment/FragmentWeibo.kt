package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.data.weibo.WeiboComment
import com.yinlin.rachel.databinding.FragmentWeiboBinding
import com.yinlin.rachel.databinding.ItemWeiboBinding
import com.yinlin.rachel.databinding.ItemWeiboCommentBinding
import com.yinlin.rachel.databinding.ItemWeiboSubcommentBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelHeaderAdapter
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter

class FragmentWeibo(pages: RachelPages, private val weibo: Weibo) : RachelFragment<FragmentWeiboBinding>(pages) {
    class SubCommentAdapter(private val pages: RachelPages, private val rilNet: RachelImageLoader) : RachelAdapter<ItemWeiboSubcommentBinding, WeiboComment>() {
        override fun bindingClass() = ItemWeiboSubcommentBinding::class.java

        override fun init(holder: RachelViewHolder<ItemWeiboSubcommentBinding>, v: ItemWeiboSubcommentBinding) {
            v.avatar.rachelClick {
                val item = this[holder.bindingAdapterPosition]
                pages.navigate(FragmentWeiboUser(pages, item.user.userId))
            }
            v.text.setOnClickATagListener { _, _, _ -> true }
        }

        override fun update(v: ItemWeiboSubcommentBinding, item: WeiboComment, position: Int) {
            v.name.text = item.user.name
            v.avatar.load(rilNet, item.user.avatar, Config.cache_daily_pic)
            v.time.text = item.time
            v.location.text = item.user.location
            v.text.setHtml(item.text, HtmlHttpImageGetter(v.text))
        }
    }

    class Adapter(fragment: FragmentWeibo, private val rilNet: RachelImageLoader) : RachelHeaderAdapter<ItemWeiboBinding, ItemWeiboCommentBinding, WeiboComment>() {
        private val pages = fragment.pages
        private val weibo = fragment.weibo

        override fun bindingHeaderClass() = ItemWeiboBinding::class.java
        override fun bindingItemClass() = ItemWeiboCommentBinding::class.java

        override fun initHeader(v: ItemWeiboBinding) {
            v.avatar.rachelClick { pages.navigate(FragmentWeiboUser(pages, weibo.user.userId)) }
            v.avatar.load(rilNet, weibo.user.avatar, Config.cache_daily_pic)
            v.name.text = weibo.user.name
            v.time.text = weibo.time
            v.location.text = weibo.user.location
            v.text.setOnClickATagListener { _, _, _ -> true }
            v.text.setHtml(weibo.text, HtmlHttpImageGetter(v.text))
            v.pics.listener = { position, _ -> pages.navigate(FragmentImagePreview(pages, v.pics.images, position)) }
            v.pics.images = weibo.pictures
            v.like.text = weibo.likeNum.toString()
            v.comment.text = weibo.commentNum.toString()
            v.repost.text = weibo.repostNum.toString()
        }

        override fun init(holder: RachelItemViewHolder<ItemWeiboCommentBinding>, v: ItemWeiboCommentBinding) {
            v.avatar.rachelClick {
                val item = this[holder.positionEx]
                pages.navigate(FragmentWeiboUser(pages, item.user.userId))
            }
            v.text.setOnClickATagListener { _, _, _ -> true }
            v.pic.rachelClick {
                val item = this[holder.positionEx]
                item.pic?.let {  pages.navigate(FragmentImagePreview(pages, RachelPreview(it))) }
            }
            v.list.apply {
                layoutManager = LinearLayoutManager(pages.context)
                setItemViewCacheSize(4)
                recycledViewPool.setMaxRecycledViews(0, 10)
                adapter = SubCommentAdapter(pages, rilNet)
            }
        }

        override fun update(v: ItemWeiboCommentBinding, item: WeiboComment, position: Int) {
            v.name.text = item.user.name
            v.avatar.load(rilNet, item.user.avatar, Config.cache_daily_pic)
            v.time.text = item.time
            v.location.text = item.user.location
            v.text.setHtml(item.text, HtmlHttpImageGetter(v.text))
            item.pic?.let { v.pic.load(rilNet, it) }
            v.pic.visible = item.pic != null
            item.subComments?.let {
                val subAdapter = v.list.adapter as SubCommentAdapter
                subAdapter.setSource(it)
                subAdapter.notifySource()
            }
            v.list.visible = item.subComments != null
        }
    }

    private val rilNet = RachelImageLoader(pages.context, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)
    private val mAdapter = Adapter(this, rilNet)

    override fun bindingClass() = FragmentWeiboBinding::class.java

    override fun init() {
        v.list.apply {
            layoutManager = LinearLayoutManager(pages.context)
            recycledViewPool.setMaxRecycledViews(0, 10)
            adapter = mAdapter
        }

        requestComment()
    }

    override fun back() = true

    // 刷新评论
    @NewThread
    fun requestComment() {
        lifecycleScope.launch {
            val comments = mutableListOf<WeiboComment>()
            withContext(Dispatchers.IO) { WeiboAPI.getDetails(weibo.id, comments) }
            if (comments.isNotEmpty()) {
                mAdapter.setSource(comments)
                mAdapter.notifySourceEx()
            }
        }
    }
}