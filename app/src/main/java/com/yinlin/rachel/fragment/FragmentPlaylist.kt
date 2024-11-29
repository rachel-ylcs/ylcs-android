package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LoadMusicPreview
import com.yinlin.rachel.data.music.LoadMusicPreviewList
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.databinding.FragmentPlaylistBinding
import com.yinlin.rachel.databinding.ItemMusicLineBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.strikethrough
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.view.NavigationView


class FragmentPlaylist(main: MainActivity, private val playlistNames: List<String>)
    : RachelFragment<FragmentPlaylistBinding>(main) {
    class Adapter(private val fragment: FragmentPlaylist) : RachelAdapter<ItemMusicLineBinding, LoadMusicPreview>(),
        RachelAdapter.ListTouch<LoadMusicPreview> {
        private val main = fragment.main

        override fun bindingClass() = ItemMusicLineBinding::class.java

        override fun update(v: ItemMusicLineBinding, item: LoadMusicPreview, position: Int) {
            v.name.apply {
                text = item.name
                textColor = main.rc(if (item.isDeleted) R.color.red else R.color.black)
                strikethrough = item.isDeleted
            }
            v.singer.text = item.singer
        }

        override fun onItemClicked(v: ItemMusicLineBinding, item: LoadMusicPreview, position: Int) {
            RachelDialog.choice(main, items = listOf("播放", "删除")) { when (it) {
                0 -> {
                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_START_PLAYER, Playlist(main.rs(R.string.default_playlist_name), item.id))
                    main.pop()
                }
                1 -> {
                    RachelDialog.confirm(main, content = "是否从歌单中删除\"${item.name}\"") {
                        fragment.v.tab.currentItem?.title?.let { title ->
                            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_MUSIC_FROM_PLAYLIST, title, item.id)
                            items.removeAt(position)
                            notifyItemRemoved(position)
                            if (items.isEmpty()) fragment.v.state.showEmpty("歌单空荡荡的, 快去曲库添加吧")
                        }
                    }
                }
            } }
        }

        override fun onMoved() {
            fragment.v.tab.processCurrent { _, title, _ ->
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_PLAYLIST, title, items)
            }
        }
    }
    companion object {
        const val GROUP_ADD = 0
        const val GROUP_PLAY = 1
        const val GROUP_RENAME = 2
        const val GROUP_DELETE = 3
    }

    private var adapter = Adapter(this)

    override fun bindingClass() = FragmentPlaylistBinding::class.java

    override fun init() {
        v.group.listener = { pos -> when (pos) {
            GROUP_ADD -> RachelDialog.input(main, "请输入新歌单名称", 10) {
                if (main.sendMessageForResult<Boolean>(RachelTab.music, RachelMessage.MUSIC_CREATE_PLAYLIST, it)!!) {
                    v.tab.addItem(it, true)
                }
                else tip(Tip.WARNING, "歌单已存在或名称不合法")
            }
            GROUP_PLAY -> v.tab.processCurrent { _, title, _ ->
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_START_PLAYER, title)
                main.pop()
            }
            GROUP_RENAME -> v.tab.processCurrent { position, title, _ ->
                RachelDialog.input(main, "修改歌单名称", 10) {
                    if (main.sendMessageForResult<Boolean>(RachelTab.music, RachelMessage.MUSIC_RENAME_PLAYLIST, title, it)!!)
                        v.tab.setItemTitle(position, it)
                    else tip(Tip.WARNING, "歌单已存在或名称不合法")
                }
            }
            GROUP_DELETE -> v.tab.processCurrent { position, title, _ ->
                RachelDialog.confirm(main, content="是否删除歌单\"${title}\"") {
                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_PLAYLIST, title)
                    v.tab.removeItem(position)
                    if (v.tab.isEmpty) v.state.showEmpty("快去创建一个歌单吧")
                }
            }
        } }

        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(main, RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
            adapter = this@FragmentPlaylist.adapter
            this@FragmentPlaylist.adapter.setListTouch(this, canSwipe = false, callback = this@FragmentPlaylist.adapter)
        }

        // TAB
        v.tab.listener = object : NavigationView.Listener {
            override fun onSelected(position: Int, title: String, obj: Any?) {
                val items = main.sendMessageForResult<LoadMusicPreviewList>(RachelTab.music, RachelMessage.MUSIC_GET_PLAYLIST_INFO_PREVIEW, title)!!
                if (items.items.isEmpty()) v.state.showEmpty("歌单空荡荡的, 快去曲库添加吧")
                else {
                    adapter.setSource(items.items)
                    adapter.notifySource()
                    v.state.showContent()
                }
            }
        }
        v.tab.simpleItems = playlistNames
        if (playlistNames.isEmpty()) v.state.showEmpty("快去创建一个歌单吧")
    }

    override fun back() = BackState.POP
}