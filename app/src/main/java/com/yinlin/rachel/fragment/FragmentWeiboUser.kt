package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.data.weibo.WeiboAlbum
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.databinding.FragmentWeiboUserBinding
import com.yinlin.rachel.databinding.ItemWeiboAlbumBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadDaily
import com.yinlin.rachel.page.common.WeiboAdapter
import com.yinlin.rachel.rachelClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentWeiboUser(main: MainActivity, private val weiboUserId: String) : RachelFragment<FragmentWeiboUserBinding>(main) {
    class WeiboAlbumAdapter(private val fragment: FragmentWeiboUser) : RachelAdapter<ItemWeiboAlbumBinding, WeiboAlbum>() {
        override fun bindingClass() = ItemWeiboAlbumBinding::class.java

        override fun update(v: ItemWeiboAlbumBinding, item: WeiboAlbum, position: Int) {
            v.title.text = item.title
            v.num.text = item.num
            v.time.text = item.time
            v.pic.loadDaily(item.pic)
        }

        override fun onItemClicked(v: ItemWeiboAlbumBinding, item: WeiboAlbum, position: Int) {
            fragment.main.navigate(FragmentWeiboAlbum(fragment.main, item.containerId))
        }
    }

    private val albumAdapter = WeiboAlbumAdapter(this)
    private val weiboAdapter = WeiboAdapter(main)

    override fun bindingClass() = FragmentWeiboUserBinding::class.java

    override fun init() {
        v.add.rachelClick {
            val weiboUserStorage: WeiboUserStorage? = Config.weibo_users.find { it.userId == weiboUserId }
            if (weiboUserStorage != null) tip(Tip.WARNING, "${weiboUserStorage.name} 已存在")
            else addWeiboUser(weiboUserId)
        }

        v.albumList.apply {
            layoutManager = LinearLayoutManager(main)
            setItemViewCacheSize(0)
            adapter = albumAdapter
        }

        v.weiboList.apply {
            layoutManager = LinearLayoutManager(main)
            setItemViewCacheSize(0)
            adapter = weiboAdapter
        }

        v.albumState.showLoading()
        v.weiboState.showLoading()
        requestUserInfo()
    }

    override fun back() = true

    @NewThread
    fun requestUserInfo() {
        lifecycleScope.launch {
            val loading = main.loading
            val userInfo = withContext(Dispatchers.IO) { WeiboAPI.extractWeiboUser(weiboUserId) }
            loading.dismiss()
            if (userInfo != null) {
                v.name.text = userInfo.name
                v.avatar.loadDaily(userInfo.avatar)
                v.bg.loadDaily(userInfo.background)
                v.signature.text = userInfo.signature
                v.follow.text = "关注 ${userInfo.followNum}"
                v.fans.text = "粉丝 ${userInfo.fansNum}"

                val albums = withContext(Dispatchers.IO) { WeiboAPI.extractWeiboUserAlbum(weiboUserId) }
                albumAdapter.setSource(albums)
                albumAdapter.notifySource()
                if (albums.isEmpty()) v.albumState.showEmpty()
                else v.albumState.showContent()

                val weibos = mutableListOf<Weibo>()
                withContext(Dispatchers.IO) { WeiboAPI.extractAllWeibo(weiboUserId, weibos) }
                weiboAdapter.setSource(weibos)
                weiboAdapter.notifySource()
                if (albums.isEmpty()) v.weiboState.showEmpty()
                else v.weiboState.showContent()
            }
            else {
                main.pop()
                tip(Tip.ERROR, "用户不存在")
            }
        }
    }

    // 添加微博用户
    @NewThread
    private fun addWeiboUser(uid: String) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { WeiboAPI.extractWeiboUserStorage(uid) }
            loading.dismiss()
            if (result != null) {
                val weiboUsers = Config.weibo_users
                weiboUsers += result
                Config.weibo_users = weiboUsers
                tip(Tip.SUCCESS, "添加成功")
            }
            else tip(Tip.ERROR, "解析微博用户失败")
        }
    }
}