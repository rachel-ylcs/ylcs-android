package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.data.music.PlaylistPreview
import com.yinlin.rachel.databinding.FragmentPlaylistBinding
import com.yinlin.rachel.databinding.ItemMusicLineBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.strikethrough
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.view.NavigationView

@Layout(FragmentPlaylistBinding::class)
class FragmentPlaylist(main: MainActivity, private val playlistNames: List<String>) : RachelFragment<FragmentPlaylistBinding>(main) {
    @Layout(ItemMusicLineBinding::class)
    class Adapter(private val fragment: FragmentPlaylist) : RachelAdapter<ItemMusicLineBinding, PlaylistPreview.MusicItem>(),
        RachelAdapter.ListTouch<PlaylistPreview.MusicItem> {
        private val main = fragment.main
        private val deletedColor = main.rc(R.color.red)
        private val normalColor = main.rc(R.color.black)

        override fun update(v: ItemMusicLineBinding, item: PlaylistPreview.MusicItem, position: Int) {
            v.name.apply {
                text = item.name
                textColor = if (item.isDeleted) deletedColor else normalColor
                strikethrough = item.isDeleted
            }
            v.singer.text = item.singer
        }

        override fun onItemClicked(v: ItemMusicLineBinding, item: PlaylistPreview.MusicItem, position: Int) {
            RachelDialog.choice(main, callbacks = listOf(
                "播放" to {
                    if (item.isDeleted) fragment.tip(Tip.WARNING, main.rs(R.string.no_audio_source))
                    else fragment.v.tab.withCurrent { _, title, _ ->
                        val playlist = main.sendMessageForResult<Playlist>(RachelTab.music, RachelMessage.MUSIC_FIND_PLAYLIST, title)
                        if (playlist != null) {
                            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_START_PLAYER, playlist, item.id)
                            main.pop()
                        }
                    }
                },
                "删除" to {
                    fragment.v.tab.withCurrent { _, title, _ ->
                        RachelDialog.confirm(main, content = "是否从歌单中删除\"${item.name}\"") {
                            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_MUSIC_FROM_PLAYLIST, title, item.id)
                            items.removeAt(position)
                            notifyItemRemoved(position)
                            if (items.isEmpty()) fragment.v.state.showEmpty()
                        }
                    }
                }
            ))
        }

        override fun onMoveCompleted(oldPosition: Int, newPosition: Int) {
            fragment.v.tab.withCurrent { _, title, _ ->
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_MOVE_MUSIC_IN_PLAYLIST, title, oldPosition, newPosition)
            }
        }
    }

    companion object {
        const val GROUP_ADD = 0
        const val GROUP_PLAY = 1
        const val GROUP_RENAME = 2
        const val GROUP_DELETE = 3
    }

    private var mAdapter = Adapter(this)

    override fun init() {
        v.group.listener = { pos -> when (pos) {
            GROUP_ADD -> RachelDialog.input(main, "请输入新歌单名称", 10) {
                if (main.sendMessageForResult<Boolean>(RachelTab.music, RachelMessage.MUSIC_CREATE_PLAYLIST, it)!!) {
                    v.tab.addItem(it, true)
                }
                else tip(Tip.WARNING, "歌单已存在或名称不合法")
            }
            GROUP_PLAY -> v.tab.withCurrent { _, title, _ ->
                val playlist = main.sendMessageForResult<Playlist>(RachelTab.music, RachelMessage.MUSIC_FIND_PLAYLIST, title)
                if (playlist != null) {
                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_START_PLAYER, playlist)
                    main.pop()
                }
                else tip(Tip.WARNING, "歌单不存在")
            }
            GROUP_RENAME -> v.tab.withCurrent { position, title, _ ->
                RachelDialog.input(main, "修改歌单名称", 10) {
                    if (main.sendMessageForResult<Boolean>(RachelTab.music, RachelMessage.MUSIC_RENAME_PLAYLIST, title, it)!!)
                        v.tab.setItemTitle(position, it)
                    else tip(Tip.WARNING, "歌单已存在或名称不合法")
                }
            }
            GROUP_DELETE -> v.tab.withCurrent { position, title, _ ->
                RachelDialog.confirm(main, content="是否删除歌单\"${title}\"") {
                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_PLAYLIST, title)
                    v.tab.removeItem(position)
                    if (v.tab.isEmpty) v.state.showEmpty()
                }
            }
        } }

        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(main, RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
            mAdapter.setListTouch(this, canSwipe = false, callback = mAdapter)
            adapter = mAdapter
        }

        // TAB
        v.tab.listener = object : NavigationView.Listener {
            override fun onSelected(position: Int, title: String, obj: Any?) {
                val playlistPreview = main.sendMessageForResult<PlaylistPreview>(RachelTab.music, RachelMessage.MUSIC_GET_PLAYLIST_PREVIEW, title)!!
                if (playlistPreview.items.isEmpty()) v.state.showEmpty()
                else {
                    mAdapter.setSource(playlistPreview.items)
                    mAdapter.notifySource()
                    v.state.showContent()
                }
            }
        }

        v.tab.simpleItems = playlistNames
        if (playlistNames.isEmpty()) v.state.showEmpty()
    }

    override fun back() = BackState.POP
}