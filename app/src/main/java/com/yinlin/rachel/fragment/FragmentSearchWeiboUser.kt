package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.data.weibo.WeiboUser
import com.yinlin.rachel.databinding.FragmentSearchWeiboUserBinding
import com.yinlin.rachel.databinding.ItemWeiboUserBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader

class FragmentSearchWeiboUser(main: MainActivity, private val items: List<WeiboUser>) : RachelFragment<FragmentSearchWeiboUserBinding>(main) {
    class Adapter(private val fragment: FragmentSearchWeiboUser) : RachelAdapter<ItemWeiboUserBinding, WeiboUser>() {
        private val rilNet = RachelImageLoader(fragment.main, R.drawable.placeholder_pic, DiskCacheStrategy.ALL)
        init { setSource(fragment.items) }

        override fun bindingClass() = ItemWeiboUserBinding::class.java

        override fun update(v: ItemWeiboUserBinding, item: WeiboUser, position: Int) {
            v.avatar.load(rilNet, item.avatar)
            v.name.text = item.name
        }

        override fun onItemClicked(v: ItemWeiboUserBinding, item: WeiboUser, position: Int) {
            fragment.main.navigate(FragmentWeiboUser(fragment.main, item.userId))
        }
    }

    private val adapter = Adapter(this)

    override fun bindingClass() = FragmentSearchWeiboUserBinding::class.java

    override fun init() {
        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(main)
            setHasFixedSize(true)
            adapter = this@FragmentSearchWeiboUser.adapter
        }
    }

    override fun back() = true
}