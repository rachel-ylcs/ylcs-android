package com.yinlin.rachel.fragment

import android.view.Gravity
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.data.weibo.WeiboComment
import com.yinlin.rachel.databinding.FragmentWeiboBinding
import com.yinlin.rachel.databinding.ItemWeiboBinding
import com.yinlin.rachel.databinding.ItemWeiboCommentBinding
import com.yinlin.rachel.databinding.ItemWeiboSubcommentBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelHeaderAdapter
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelImageLoader.loadDaily
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.tool.toDP
import com.yinlin.rachel.view.LoadingTextView
import com.yinlin.rachel.tool.visible
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter

@Layout(FragmentWeiboBinding::class)
class FragmentWeibo(main: MainActivity, private val weibo: Weibo) : RachelFragment<FragmentWeiboBinding>(main) {
    class SubCommentAdapter(private val main: MainActivity) : RachelAdapter<ItemWeiboSubcommentBinding, WeiboComment>() {
        override fun bindingClass() = ItemWeiboSubcommentBinding::class.java

        override fun init(holder: RachelViewHolder<ItemWeiboSubcommentBinding>, v: ItemWeiboSubcommentBinding) {
            v.avatar.rachelClick {
                val item = this[holder.bindingAdapterPosition]
                main.navigate(FragmentWeiboUser(main, item.user.userId))
            }
            v.text.setOnClickATagListener { _, _, _ -> true }
        }

        override fun update(v: ItemWeiboSubcommentBinding, item: WeiboComment, position: Int) {
            v.name.text = item.user.name
            v.avatar.loadDaily(item.user.avatar)
            v.time.text = item.time
            v.location.text = item.user.location
            v.text.setHtml(item.text, HtmlHttpImageGetter(v.text))
        }
    }

    class Adapter(private val fragment: FragmentWeibo) : RachelHeaderAdapter<ItemWeiboBinding, ItemWeiboCommentBinding, WeiboComment>() {
        private val main = fragment.main
        private val weibo = fragment.weibo

        override fun bindingHeaderClass() = ItemWeiboBinding::class.java
        override fun bindingItemClass() = ItemWeiboCommentBinding::class.java

        override fun initHeader(v: ItemWeiboBinding) {
            val loading = LoadingTextView(v.contentContainer.context).apply {
                layoutParams = MarginLayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 5.toDP(context), 0, 0)
                }
                gravity = Gravity.CENTER
            }
            v.contentContainer.addView(loading)
            v.avatar.rachelClick { main.navigate(FragmentWeiboUser(main, weibo.user.userId)) }
            v.avatar.loadDaily(weibo.user.avatar)
            v.name.text = weibo.user.name
            v.time.text = weibo.time
            v.location.text = weibo.user.location
            v.text.setOnClickATagListener { _, _, _ -> true }
            v.text.setHtml(weibo.text, HtmlHttpImageGetter(v.text))
            v.pics.listener = { position, _ -> main.navigate(FragmentImagePreview(main, v.pics.images, position)) }
            v.pics.images = weibo.pictures
            v.like.text = weibo.likeNum.toString()
            v.comment.text = weibo.commentNum.toString()
            v.repost.text = weibo.repostNum.toString()

            requestComment(loading)
        }

        override fun init(holder: RachelItemViewHolder<ItemWeiboCommentBinding>, v: ItemWeiboCommentBinding) {
            v.avatar.rachelClick {
                val item = this[holder.positionEx]
                main.navigate(FragmentWeiboUser(main, item.user.userId))
            }
            v.text.setOnClickATagListener { _, _, _ -> true }
            v.pic.rachelClick {
                val item = this[holder.positionEx]
                item.pic?.let {  main.navigate(FragmentImagePreview(main, it)) }
            }
            v.list.apply {
                layoutManager = LinearLayoutManager(main)
                setItemViewCacheSize(4)
                recycledViewPool.setMaxRecycledViews(0, 10)
                adapter = SubCommentAdapter(main)
            }
        }

        override fun update(v: ItemWeiboCommentBinding, item: WeiboComment, position: Int) {
            v.name.text = item.user.name
            v.avatar.loadDaily(item.user.avatar)
            v.time.text = item.time
            v.location.text = item.user.location
            v.text.setHtml(item.text, HtmlHttpImageGetter(v.text))
            item.pic?.let { v.pic.load(it.mImageUrl) }
            v.pic.visible = item.pic != null
            item.subComments?.let {
                val subAdapter = v.list.adapter as SubCommentAdapter
                subAdapter.setSource(it)
                subAdapter.notifySource()
            }
            v.list.visible = item.subComments != null
        }

        @IOThread
        private fun requestComment(loading: LoadingTextView) {
            loading.loading = true
            val comments = mutableListOf<WeiboComment>()
            fragment.startIOWithResult({ WeiboAPI.extractWeiboDetails(weibo.id, comments) }) {
                loading.loading = false
                if (comments.isNotEmpty()) {
                    setSource(comments)
                    notifySourceEx()
                }
            }
        }
    }

    private val mAdapter = Adapter(this)

    override fun init() {
        v.list.apply {
            layoutManager = LinearLayoutManager(main)
            recycledViewPool.setMaxRecycledViews(0, 10)
            adapter = mAdapter
        }
    }

    override fun back() = BackState.POP
}