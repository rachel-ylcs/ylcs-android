package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LoadMusicPreview
import com.yinlin.rachel.data.music.LoadMusicPreviewList
import com.yinlin.rachel.databinding.FragmentPlaylistBinding
import com.yinlin.rachel.databinding.ItemMusicLineBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.strikethrough
import com.yinlin.rachel.textColor
import com.yinlin.rachel.tip


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

        override fun onMoved() {
            fragment.v.tab.processCurrentTabEx { _, title, _ ->
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_PLAYLIST, title, items)
            }
        }

        override fun onRemove(item: LoadMusicPreview, position: Int) {
            fragment.v.tab.processCurrentTabEx { _, title, _ ->
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_MUSIC_FROM_PLAYLIST, title, item.id)
                if (items.isEmpty()) fragment.v.state.showEmpty("歌单空荡荡的, 快去曲库添加吧")
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
                    v.tab.addTabEx(it)
                    v.tab.selectTabEx(v.tab.tabCount - 1)
                    v.tab.scrollToTabEx(v.tab.tabCount - 1)
                }
                else tip(Tip.WARNING, "歌单已存在或名称不合法")
            }
            GROUP_PLAY -> v.tab.processCurrentTabEx { _, title, _ ->
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_START_PLAYER, title)
                main.pop()
            }
            GROUP_RENAME -> v.tab.processCurrentTabEx { view, title, _ ->
                RachelDialog.input(main, "修改歌单名称", 10) {
                    if (main.sendMessageForResult<Boolean>(RachelTab.music, RachelMessage.MUSIC_RENAME_PLAYLIST, title, it)!!)
                        view.text = it
                    else tip(Tip.WARNING, "歌单已存在或名称不合法")
                }
            }
            GROUP_DELETE -> v.tab.processCurrentTabEx { _, title, position ->
                RachelDialog.confirm(main, content="是否删除歌单\"${title}\"") {
                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_PLAYLIST, title)
                    v.tab.removeTabEx(position)
                    if (v.tab.isEmpty) {
                        adapter.clearSource()
                        adapter.notifySource()
                        v.state.showEmpty("快去创建一个歌单吧")
                    }
                }
            }
        } }

        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(main, RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
            adapter = this@FragmentPlaylist.adapter
            this@FragmentPlaylist.adapter.setListTouch(this, this@FragmentPlaylist.adapter)
        }

        // TAB
        v.tab.listener = TabListener@ { _, title ->
            val items = main.sendMessageForResult<LoadMusicPreviewList>(RachelTab.music, RachelMessage.MUSIC_GET_PLAYLIST_INFO_PREVIEW, title)
            if (items != null) {
                adapter.setSource(items)
                adapter.notifySource()
                if (adapter.isNotEmpty) {
                    v.state.showContent()
                    return@TabListener
                }
            }
            v.state.showEmpty("歌单空荡荡的, 快去曲库添加吧")
        }

        for (title in playlistNames) v.tab.addTabEx(title)
        if (playlistNames.isNotEmpty()) v.tab.selectTabEx(0)
        else v.state.showEmpty("快去创建一个歌单吧")
    }

    override fun back() = true
}