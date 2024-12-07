package com.yinlin.rachel.fragment

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.attachAlpha
import com.yinlin.rachel.tool.backgroundColor
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.MusicRes
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.databinding.FragmentMusicInfoBinding
import com.yinlin.rachel.databinding.ItemMusicResBinding
import com.yinlin.rachel.tool.fileSizeString
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelMod
import com.yinlin.rachel.model.RachelPictureSelector
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.model.engine.LineLyricsEngine
import com.yinlin.rachel.model.engine.PAGLyricsEngine
import com.yinlin.rachel.tool.copySafely
import com.yinlin.rachel.tool.deleteSafely
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.pathMusic
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.readText
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FragmentMusicInfo(main: MainActivity, private val musicInfo: MusicInfo) : RachelFragment<FragmentMusicInfoBinding>(main) {
    class Adapter(private val fragment: FragmentMusicInfo) : RachelAdapter<ItemMusicResBinding, MusicRes>() {
        private val main = fragment.main

        override fun bindingClass() = ItemMusicResBinding::class.java

        override fun init(holder: RachelViewHolder<ItemMusicResBinding>, v: ItemMusicResBinding) {
            v.edit.rachelClick {
                val pos = holder.bindingAdapterPosition
                val item = this[pos]
                if (item.canEdit) fragment.editRes(pos, item)
            }

            v.delete.rachelClick {
                val pos = holder.bindingAdapterPosition
                val item = this[pos]
                if (item.canDelete) RachelDialog.confirm(main, "该操作不可逆!", "是否从从MOD中删除该资源") {
                    fragment.deleteRes(pos, item)
                }
            }
        }

        override fun update(v: ItemMusicResBinding, item: MusicRes, position: Int) {
            v.description.text = item.description
            v.fs.text = item.fileSize
            v.name.text = item.name
            v.root.backgroundColor = main.rc(item.color).attachAlpha(180)
            v.edit.visible = item.canEdit
            v.delete.visible = item.canDelete
        }
    }

    private val mAdapter = Adapter(this)

    companion object {
        const val GROUP_PLAY = 0
        const val GROUP_ADD = 1
        const val GROUP_SHARE = 2
    }

    override fun bindingClass() = FragmentMusicInfoBinding::class.java

    override fun init() {
        v.name.rachelClick {
            RachelDialog.input(main, "修改歌曲名称", 32) {
                v.name.text = it
                musicInfo.name = it
                main.sendBottomMessage(FragmentLibrary::class, RachelMessage.LIBRARY_UPDATE_MUSIC_INFO, musicInfo)
                val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                if (currentMusicInfo == musicInfo) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_MUSIC_INFO, musicInfo)
                musicInfo.rewrite()
            }
        }

        v.singer.rachelClick {
            RachelDialog.input(main, "修改演唱歌手", 32) {
                v.singer.text = it
                musicInfo.singer = it
                main.sendBottomMessage(FragmentLibrary::class, RachelMessage.LIBRARY_UPDATE_MUSIC_INFO, musicInfo)
                val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                if (currentMusicInfo == musicInfo) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_MUSIC_INFO, musicInfo)
                musicInfo.rewrite()
            }
        }

        v.lyricist.rachelClick {
            RachelDialog.input(main, "修改作词", 32) {
                v.lyricist.text = it
                musicInfo.lyricist = it
                musicInfo.rewrite()
            }
        }

        v.composer.rachelClick {
            RachelDialog.input(main, "修改作曲", 32) {
                v.composer.text = it
                musicInfo.composer = it
                musicInfo.rewrite()
            }
        }

        v.album.rachelClick {
            RachelDialog.input(main, "修改专辑分类", 32) {
                v.album.text = it
                musicInfo.album = it
                musicInfo.rewrite()
            }
        }

        v.name.text = musicInfo.name
        v.version.text = musicInfo.version
        v.id.text = "ID: ${musicInfo.id}"
        v.author.text = "MOD来源: ${musicInfo.author}"
        v.record.load(musicInfo.recordPath)
        v.singer.text = "演唱: ${musicInfo.singer}"
        v.lyricist.text = "作词: ${musicInfo.lyricist}"
        v.composer.text = "作曲: ${musicInfo.composer}"
        v.album.text = "专辑分类: ${musicInfo.album}"

        v.list.apply {
            layoutManager = object : LinearLayoutManager(main, RecyclerView.VERTICAL, false) {
                override fun canScrollVertically() = false
            }
            setItemViewCacheSize(0)
            adapter = mAdapter
        }

        v.groupRes.listener = { pos -> when (pos) {
            GROUP_PLAY -> main.sendMessage(RachelTab.music, RachelMessage.MUSIC_START_PLAYER, Playlist(main.rs(R.string.default_playlist_name), musicInfo.id))
            GROUP_ADD -> { }
            GROUP_SHARE -> RachelDialog.confirm(main, content="导出MOD\"${this.id}\"到文件分享?") { shareMusic(musicInfo.id) }
        } }

        lifecycleScope.launch {
            v.loadingLyrics.loading = true
            if (musicInfo.lrcData == null) {
                val lrcData = withContext(Dispatchers.IO) { LrcData.parseLrcData(musicInfo.defaultLrcPath.readText()) }
                musicInfo.lrcData = lrcData
            }
            v.loadingLyrics.loading = false
            musicInfo.lrcData?.let { v.lyrics.text = it.plainText }
        }

        lifecycleScope.launch {
            v.loadingRes.loading = true
            withContext(Dispatchers.IO) {
                val resList = pathMusic.listFiles {
                    file -> MusicRes.checkResFile(musicInfo.id, file)
                }?.map { MusicRes.parse(musicInfo.id, it) } ?: emptyList()
                mAdapter.setSource(resList)
                mAdapter.notifySource()
            }
            v.loadingRes.loading = false
        }
    }

    override fun back() = BackState.POP

    private fun editRes(pos: Int, res: MusicRes) = when (res.id) {
        MusicRes.Type.RECORD -> RachelPictureSelector.single(main, 512, 512, false) {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val result = File(it).copySafely(musicInfo.recordPath)
                    if (result) {
                        withContext(Dispatchers.Main) {
                            main.sendBottomMessage(FragmentLibrary::class, RachelMessage.LIBRARY_UPDATE_MUSIC_INFO, musicInfo)
                            val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                            if (currentMusicInfo == musicInfo) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_MUSIC_INFO, musicInfo)
                        }
                    }
                    result
                }
                if (result) {
                    v.record.load(musicInfo.recordPath)
                    res.fileSize = musicInfo.recordPath.fileSizeString
                    mAdapter.notifyItemChanged(pos)
                }
                else tip(Tip.ERROR, "编辑失败")
            }
        }
        MusicRes.Type.BGS -> RachelPictureSelector.single(main, 1080, 1920, false) {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val result = File(it).copySafely(musicInfo.bgsPath)
                    if (result) {
                        withContext(Dispatchers.Main) {
                            val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                            if (currentMusicInfo == musicInfo) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_MUSIC_INFO, musicInfo)
                        }
                    }
                    result
                }
                if (result) {
                    res.fileSize = musicInfo.bgsPath.fileSizeString
                    mAdapter.notifyItemChanged(pos)
                }
                else tip(Tip.ERROR, "编辑失败")
            }
        }
        else -> tip(Tip.WARNING, "该资源未开放编辑")
    }

    private fun deleteRes(pos: Int, res: MusicRes) {
        when (res.id) {
            MusicRes.Type.VIDEO -> lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val result = musicInfo.videoPath.deleteSafely()
                    if (result) {
                        musicInfo.video = false
                        musicInfo.rewrite()
                        withContext(Dispatchers.Main) {
                            val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                            if (currentMusicInfo == musicInfo) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_MUSIC_INFO, musicInfo)
                        }
                    }
                    result
                }
                if (result) {
                    mAdapter.removeItem(pos)
                    mAdapter.notifyItemRemoved(pos)
                }
            }
            MusicRes.Type.BGD -> lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val result = musicInfo.bgdPath.deleteSafely()
                    if (result) {
                        musicInfo.bgd = false
                        musicInfo.rewrite()
                        withContext(Dispatchers.Main) {
                            val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                            if (currentMusicInfo == musicInfo) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_MUSIC_INFO, musicInfo)
                        }
                    }
                    result
                }
                if (result) {
                    mAdapter.removeItem(pos)
                    mAdapter.notifyItemRemoved(pos)
                }
            }
            MusicRes.Type.LRC -> lifecycleScope.launch {
                val lrcList = musicInfo.lyrics[LineLyricsEngine.NAME]
                val resId = res.name.removeSuffix(res.ext)
                if (resId.isNotEmpty() && lrcList != null && lrcList.contains(resId)) {
                    val result = withContext(Dispatchers.IO) {
                        val result = musicInfo.customPath(res.name).deleteSafely()
                        if (result) {
                            lrcList.remove(resId)
                            musicInfo.rewrite()
                            // 如果正在播放这首歌则切换到默认歌词
                            withContext(Dispatchers.Main) {
                                val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                                if (currentMusicInfo == musicInfo)
                                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_USE_LYRICS_ENGINE, LineLyricsEngine.NAME, LineLyricsEngine.DEFAULT_RES)
                            }
                        }
                        result
                    }
                    if (result) {
                        mAdapter.removeItem(pos)
                        mAdapter.notifyItemRemoved(pos)
                    }
                }
            }
            MusicRes.Type.PAG -> lifecycleScope.launch {
                val pagList = musicInfo.lyrics[PAGLyricsEngine.NAME]
                val resId = res.name.removeSuffix(res.ext)
                if (resId.isNotEmpty() && pagList != null && pagList.contains(resId)) {
                    val result = withContext(Dispatchers.IO) {
                        val result = musicInfo.customPath(res.name).deleteSafely()
                        if (result) {
                            pagList.remove(resId)
                            if (pagList.isEmpty()) musicInfo.lyrics.remove(PAGLyricsEngine.NAME)
                            musicInfo.rewrite()
                            // 如果正在播放这首歌则切换到默认歌词
                            withContext(Dispatchers.Main) {
                                val currentMusicInfo = main.sendMessageForResult<MusicInfo>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_MUSIC_INFO)
                                if (currentMusicInfo == musicInfo)
                                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_USE_LYRICS_ENGINE, LineLyricsEngine.NAME, LineLyricsEngine.DEFAULT_RES)
                            }
                        }
                        result
                    }
                    if (result) {
                        mAdapter.removeItem(pos)
                        mAdapter.notifyItemRemoved(pos)
                    }
                }
            }
            else -> { }
        }
    }

    // 分享歌曲
    @IOThread
    private fun shareMusic(id: String) {
        lifecycleScope.launch {
            val loading = main.loading
            val uri = withContext(Dispatchers.IO) {
                try {
                    val merger = RachelMod.Merger(pathMusic)
                    val metadata = merger.getMetadata(listOf(id), emptyList(), null)
                    val resolver: ContentResolver = main.contentResolver
                    val values = ContentValues()
                    values.put(MediaStore.Downloads.DISPLAY_NAME, "${id}.rachel")
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                    if (resolver.openOutputStream(uri).use { merger.run(it!!, metadata, null) }) uri
                    else null
                } catch (_: Exception) { null }
            }
            loading.dismiss()
            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "application/*"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(intent, "分享歌曲MOD"))
            }
            catch (_: Exception) {
                tip(Tip.ERROR, "导出MOD失败")
            }
        }
    }
}