package com.yinlin.rachel.fragment

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.api.WeiboAPI
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.weibo.WeiboUserStorage
import com.yinlin.rachel.databinding.FragmentWeiboUserListBinding
import com.yinlin.rachel.databinding.ItemWeiboUserBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.loadDaily
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.tool.visible
import com.yinlin.rachel.tool.withIO
import kotlinx.coroutines.launch

@Layout(FragmentWeiboUserListBinding::class)
class FragmentWeiboUserList(main: MainActivity) : RachelFragment<FragmentWeiboUserListBinding>(main) {
    @Layout(ItemWeiboUserBinding::class)
    class Adapter(fragment: FragmentWeiboUserList) : RachelAdapter<ItemWeiboUserBinding, WeiboUserStorage>() {
        private val main = fragment.main
        var isManagerMode = true

        override fun init(holder: RachelViewHolder<ItemWeiboUserBinding>, v: ItemWeiboUserBinding) {
            v.delete.rachelClick {
                val pos = holder.bindingAdapterPosition
                RachelDialog.confirm(main, content="是否删除此微博用户") {
                    val weiboUsers = Config.weibo_users
                    weiboUsers.removeAt(pos)
                    Config.weibo_users = weiboUsers
                    removeItem(pos)
                    notifyItemRemoved(pos)
                }
            }
        }

        override fun update(v: ItemWeiboUserBinding, item: WeiboUserStorage, position: Int) {
            v.avatar.loadDaily(item.avatar)
            v.name.text = item.name
            v.delete.visible = isManagerMode
        }

        override fun onItemClicked(v: ItemWeiboUserBinding, item: WeiboUserStorage, position: Int) {
            main.navigate(FragmentWeiboUser(main, item.userId))
        }
    }

    private val mAdapter = Adapter(this)

    companion object {
        const val GROUP_SEARCH = 0
        const val GROUP_REFRESH = 1
    }

    override fun init() {
        v.group.listener = { pos -> when (pos) {
            GROUP_SEARCH -> RachelDialog.input(main, "输入微博用户的昵称", 24) { searchWeiboUser(it) }
            GROUP_REFRESH -> loadWeiboUserStorage()
        } }

        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(main)
            setHasFixedSize(true)
            adapter = mAdapter
        }

        loadWeiboUserStorage()
    }

    override fun back() = BackState.POP

    @IOThread
    private fun loadWeiboUserStorage() {
        lifecycleScope.launch {
            v.state.showLoading()
            val users = Config.weibo_users
            val correctUsers = mutableListOf<WeiboUserStorage>()
            var isNeedUpdate = false
            for (user in users) {
                if (user.name.isEmpty() || user.avatar.isEmpty()) {
                    val userStorage = withIO { WeiboAPI.extractWeiboUserStorage(user.userId) }
                    if (userStorage != null) {
                        user.name = userStorage.name
                        user.avatar = userStorage.avatar
                        correctUsers += user
                        isNeedUpdate = true
                    }
                }
                else correctUsers += user
            }
            if (isNeedUpdate) Config.weibo_users = users
            if (correctUsers.isEmpty()) v.state.showEmpty()
            else {
                v.title.text = main.rs(R.string.weibo_user_list_like)
                mAdapter.isManagerMode = true
                mAdapter.setSource(correctUsers)
                mAdapter.notifySource()
                v.state.showContent()
            }
        }
    }

    @IOThread
    private fun searchWeiboUser(name: String) {
        v.state.showLoading()
        startIOWithResult({ WeiboAPI.searchUser(name) }) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    v.title.text = main.rs(R.string.weibo_user_list_search)
                    mAdapter.isManagerMode = false
                    mAdapter.setSource(it)
                    mAdapter.notifySource()
                    v.state.showContent()
                }
                else v.state.showEmpty()
            }
            else v.state.showOffline { searchWeiboUser(name) }
        }
    }
}