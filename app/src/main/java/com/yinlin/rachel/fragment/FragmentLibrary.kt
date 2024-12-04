package com.yinlin.rachel.fragment

import androidx.recyclerview.widget.GridLayoutManager
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.tool.backgroundColor
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.MusicInfoPreview
import com.yinlin.rachel.data.music.MusicInfoPreviewList
import com.yinlin.rachel.databinding.FragmentLibraryBinding
import com.yinlin.rachel.databinding.ItemMusicBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.visible


class FragmentLibrary(main: MainActivity, private val musicInfoPreviews: MusicInfoPreviewList)
    : RachelFragment<FragmentLibraryBinding>(main) {
    class Adapter(private val fragment: FragmentLibrary) : RachelAdapter<ItemMusicBinding, MusicInfoPreview>() {
        private val main = fragment.main
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
            v.pic.load(item.recordPath) // 封面
            v.singer.backgroundColor = if (isManageMode && item.selected) selectedColor else 0
        }

        override fun onItemClicked(v: ItemMusicBinding, item: MusicInfoPreview, position: Int) {
            if (isManageMode) {
                item.selected = !item.selected
                if (allSource { !it.selected }) isManageMode = false
                else notifyItemChanged(position)
            }
            else {
                val musicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_MUSIC_INFO, item.id)
                musicInfo?.let { main.navigate(FragmentMusicInfo(main, musicInfo)) }
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
        const val GROUP_ADD = 0
        const val GROUP_DELETE = 1
        const val GROUP_ALL = 2
    }

    private var mAdapter = Adapter(this)

    override fun bindingClass() = FragmentLibraryBinding::class.java

    override fun init() {
        v.groupLeft.apply {
            listener = { pos -> when (pos) {
                GROUP_SEARCH -> RachelDialog.input(main, "搜索歌曲", 32) {
                    val newItems = main.sendMessageForResult<MusicInfoPreviewList>(RachelTab.music, RachelMessage.MUSIC_SEARCH_MUSIC_INFO_PREVIEW, it)!!
                    mAdapter.setSource(newItems)
                    mAdapter.notifySource()
                    v.list.smoothScrollToPosition(0)
                }
                GROUP_REFRESH -> {
                    val newItems = main.sendMessageForResult<MusicInfoPreviewList>(RachelTab.music, RachelMessage.MUSIC_SEARCH_MUSIC_INFO_PREVIEW, null)!!
                    mAdapter.setSource(newItems)
                    mAdapter.notifySource()
                    v.list.smoothScrollToPosition(0)
                }
            } }
        }
        v.groupRight.apply {
            listener = { pos -> when (pos) {
                GROUP_ADD -> addMusicIntoPlaylist()
                GROUP_DELETE -> deleteMusic()
                GROUP_ALL -> mAdapter.selectAll()
            } }
        }

        v.list.apply {
            layoutManager = GridLayoutManager(context, 3)
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
            setItemViewCacheSize(6)
            adapter = mAdapter
        }

        mAdapter.setSource(musicInfoPreviews)
        if (mAdapter.isEmpty) v.state.showEmpty("曲库空荡荡的, 快去工坊下载吧")
        else v.state.showContent()
    }

    override fun back(): BackState {
        if (mAdapter.isManageMode) {
            mAdapter.isManageMode = false
            return BackState.CANCEL
        }
        return BackState.POP
    }

    override fun message(msg: RachelMessage, vararg args: Any?) {
        when (msg) {
            // TODO:
            RachelMessage.LIBRARY_UPDATE_MUSIC_INFO -> {
                val musicInfo = args[0] as MusicInfo
                val pos = mAdapter.items.indexOfFirst { it.id == musicInfo.id }
                if (pos != -1) {
                    mAdapter.items[pos] = musicInfo.preview
                    mAdapter.notifyItemChanged(pos)
                }
            }
            else -> { }
        }
    }

    private fun setManageButtonStatus(status: Boolean) {
        v.groupLeft.visible = !status
        v.groupRight.visible = status
    }

    // 添加到歌单
    private fun addMusicIntoPlaylist() {
        val selectIds = mAdapter.checkIds
        if (selectIds.isNotEmpty()) {
            // 获得所有歌单名供选择
            val playlistNames = main.sendMessageForResult<List<String>>(RachelTab.music, RachelMessage.MUSIC_GET_PLAYLIST_NAMES)!!
            if (playlistNames.isEmpty()) tip(Tip.WARNING, "没有创建任何歌单")
            else {
                mAdapter.isManageMode = false
                RachelDialog.choice(main, "添加到歌单", playlistNames) { pos ->
                    val num = main.sendMessageForResult<Int>(RachelTab.music, RachelMessage.MUSIC_ADD_MUSIC_INTO_PLAYLIST, playlistNames[pos], selectIds)!!
                    if (num > 0) tip(Tip.SUCCESS, "已添加${num}首歌曲")
                    else tip(Tip.WARNING, "未能成功添加任何歌曲")
                }
            }
        }
    }

    // 删除歌曲
    private fun deleteMusic() {
        val selectIds = mAdapter.checkIds
        if (selectIds.isNotEmpty()) RachelDialog.confirm(main, content="是否从曲库中卸载指定歌曲?") {
            main.sendMessage(RachelTab.music, RachelMessage.MUSIC_DELETE_MUSIC, selectIds)
            for (ids in selectIds) mAdapter.items.remove(ids)
            mAdapter.isManageMode = false
        }
    }
}