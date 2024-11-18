package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.GridLayoutManager
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.backgroundColor
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.MusicInfoPreview
import com.yinlin.rachel.data.music.MusicInfoPreviewList
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.databinding.FragmentLibraryBinding
import com.yinlin.rachel.databinding.ItemMusicBinding
import com.yinlin.rachel.load
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelEnum
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tip
import com.yinlin.rachel.visible


class FragmentLibrary(main: MainActivity, private val musicInfoPreviews: MusicInfoPreviewList)
    : RachelFragment<FragmentLibraryBinding>(main) {
    class Adapter(private val main: MainActivity, private val fragment: FragmentLibrary)
        : RachelAdapter<ItemMusicBinding, MusicInfoPreview>() {

        private val selectedColor = main.rc(R.color.steel_blue_alpha)

        var isManageMode: Boolean = false
            set(value) {
                field = value
                if (!value) mapSource { it.selected = false }
                fragment.setManageButtonStatus(value)
                if (isEmpty) fragment.v.state.showEmpty("曲库空荡荡的, 快去工坊下载吧")
                else fragment.v.state.showContent()
                notifySource()
            }

        override fun bindingClass() = ItemMusicBinding::class.java

        override fun update(v: ItemMusicBinding, item: MusicInfoPreview, position: Int) {
            v.name.text = item.name // 歌名
            v.singer.text = item.singer
            v.version.text = item.version // 版本
            v.pic.load(main.ril, item.recordPath) // 封面
            v.singer.backgroundColor = if (isManageMode && item.selected) selectedColor else 0
        }

        override fun onItemClicked(v: ItemMusicBinding, item: MusicInfoPreview, position: Int) {
            if (isManageMode) {
                item.selected = !item.selected
                if (allSource { !it.selected }) isManageMode = false
                else notifyItemChanged(position)
            } else {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_START_PLAYER, Playlist(main.rs(R.string.default_playlist_name), item.id))
                main.pop()
            }
        }

        override fun onItemLongClicked(v: ItemMusicBinding, item: MusicInfoPreview, position: Int) {
            if (!isManageMode) {
                item.selected = true
                isManageMode = true
            }
        }

        fun selectAll() {
            if (isManageMode) {
                mapSource { it.selected = true }
                notifySource()
            }
        }

        // 获取所有选中歌曲的编号
        val checkIds: MusicInfoPreviewList get() = filterSource { it.selected }
    }

    companion object {
        const val GROUP_SEARCH = 0
        const val GROUP_REFRESH = 1
        const val GROUP_ADD = 2
        const val GROUP_DELETE = 3
        const val GROUP_ALL = 4
    }

    private var adapter = Adapter(main, this)

    override fun bindingClass() = FragmentLibraryBinding::class.java

    override fun init() {
        v.group.apply {
            setItemVisibility(GROUP_ADD, false)
            setItemVisibility(GROUP_DELETE, false)
            setItemVisibility(GROUP_ALL, false)
            listener = { pos -> when (pos) {
                GROUP_SEARCH -> RachelDialog.input(main, "搜索歌曲", 32) {
                    val newItems = main.sendMessageForResult<MusicInfoPreviewList>(RachelTab.music, RachelMessage.MUSIC_GET_MUSIC_INFO_PREVIEW, it)!!
                    adapter.setSource(newItems)
                    adapter.notifySource()
                }
                GROUP_REFRESH -> {
                    val newItems = main.sendMessageForResult<MusicInfoPreviewList>(RachelTab.music, RachelMessage.MUSIC_GET_MUSIC_INFO_PREVIEW, null)!!
                    adapter.setSource(newItems)
                    adapter.notifySource()
                }
                GROUP_ADD -> addMusicIntoPlaylist()
                GROUP_DELETE -> deleteMusic()
                GROUP_ALL -> adapter.selectAll()
            } }
        }

        v.list.apply {
            layoutManager = GridLayoutManager(context, 3)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
            setItemViewCacheSize(6)
            adapter = this@FragmentLibrary.adapter
        }

        adapter.setSource(musicInfoPreviews)
        if (adapter.isEmpty) v.state.showEmpty("曲库空荡荡的, 快去工坊下载吧")
        else v.state.showContent()
    }

    override fun back(): Boolean {
        if (adapter.isManageMode) {
            adapter.isManageMode = false
            return false
        }
        return true
    }

    private fun setManageButtonStatus(status: Boolean) {
        v.group.apply {
            setItemVisibility(GROUP_SEARCH, !status)
            setItemVisibility(GROUP_REFRESH, !status)
            setItemVisibility(GROUP_ADD, status)
            setItemVisibility(GROUP_DELETE, status)
            setItemVisibility(GROUP_ALL, status)
        }
    }

    // 添加到歌单
    private fun addMusicIntoPlaylist() {
        val selectIds = adapter.checkIds
        if (selectIds.isNotEmpty()) {
            adapter.isManageMode = false
            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_ADD_MUSIC_INTO_PLAYLIST, selectIds)
        }
        else tip(Tip.WARNING, "请至少选择一首歌曲")
    }

    // 删除歌曲
    private fun deleteMusic() {
        val selectIds = adapter.checkIds
        if (selectIds.isNotEmpty()) {
            RachelDialog.confirm(main, content="是否从曲库中卸载指定歌曲?") {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_MUSIC, selectIds)
                main.pop()
                tip(Tip.SUCCESS, "已卸载${selectIds.size}首歌曲")
            }
        }
        else tip(Tip.WARNING, "请至少选择一首歌曲")
    }
}