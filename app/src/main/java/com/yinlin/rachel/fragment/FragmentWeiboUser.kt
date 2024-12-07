package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.data.weibo.Weibo
import com.yinlin.rachel.data.weibo.WeiboAlbum
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.databinding.FragmentWeiboUserBinding
import com.yinlin.rachel.databinding.ItemWeiboAlbumBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadDaily
import com.yinlin.rachel.common.WeiboAdapter
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.startIOWithResult

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

        requestUserInfo()
    }

    override fun back() = BackState.POP

    @IOThread
    fun requestUserInfo() {
        v.name.loading = true
        startIOWithResult({ WeiboAPI.extractWeiboUser(weiboUserId) }) { userInfo ->
            v.name.loading = false
            if (userInfo != null) {
                v.name.text = userInfo.name
                v.avatar.loadDaily(userInfo.avatar)
                v.bg.loadDaily(userInfo.background)
                v.signature.text = userInfo.signature
                v.follow.text = "关注 ${userInfo.followNum}"
                v.fans.text = "粉丝 ${userInfo.fansNum}"

                v.loadingAlbum.loading = true
                startIOWithResult({ WeiboAPI.extractWeiboUserAlbum(weiboUserId) }) {
                    v.loadingAlbum.loading = false
                    if (it.isNotEmpty()) {
                        albumAdapter.setSource(it)
                        albumAdapter.notifySource()
                    }
                }

                v.loadingWeibo.loading = true
                val weibos = mutableListOf<Weibo>()
                startIOWithResult({ WeiboAPI.extractAllWeibo(weiboUserId, weibos) }) {
                    v.loadingWeibo.loading = false
                    if (weibos.isNotEmpty()) {
                        weiboAdapter.setSource(weibos)
                        weiboAdapter.notifySource()
                    }
                }
            }
            else {
                main.pop()
                tip(Tip.ERROR, "用户不存在")
            }
        }
    }

    // 添加微博用户
    @IOThread
    private fun addWeiboUser(uid: String) {
        val loading = main.loading
        startIOWithResult({ WeiboAPI.extractWeiboUserStorage(uid) }) {
            loading.dismiss()
            if (it != null) {
                val weiboUsers = Config.weibo_users
                weiboUsers += it
                Config.weibo_users = weiboUsers
                tip(Tip.SUCCESS, "添加成功")
            }
            else tip(Tip.ERROR, "解析微博用户失败")
        }
    }
}