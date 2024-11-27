package com.yinlin.rachel.fragment

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.attachAlpha
import com.yinlin.rachel.backgroundColor
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.data.music.LrcData
import com.yinlin.rachel.data.music.MusicInfo
import com.yinlin.rachel.data.music.MusicRes
import com.yinlin.rachel.data.music.Playlist
import com.yinlin.rachel.databinding.FragmentMusicInfoBinding
import com.yinlin.rachel.databinding.ItemMusicResBinding
import com.yinlin.rachel.gson
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelMod
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.pathMusic
import com.yinlin.rachel.pureColor
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentMusicInfo(main: MainActivity, private val musicInfo: MusicInfo) : RachelFragment<FragmentMusicInfoBinding>(main) {
    class Adapter(private val fragment: FragmentMusicInfo) : RachelAdapter<ItemMusicResBinding, MusicRes>() {
        private val main = fragment.main

        override fun bindingClass() = ItemMusicResBinding::class.java

        override fun init(holder: RachelViewHolder<ItemMusicResBinding>, v: ItemMusicResBinding) {
            v.delete.rachelClick {
                val pos = holder.bindingAdapterPosition
                val item = items[pos]
                if (!item.isBasic) {
                    RachelDialog.confirm(main, "该操作不可逆!", "是否从从MOD中删除该资源") {
                        main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
//                        if (item.onDelete?.invoke(item, fragment.musicInfo) == true) {
//                            removeItem(pos)
//                            notifyItemRemoved(pos)
//                        }
//                        else fragment.tip(Tip.ERROR, "删除失败")
                    }
                }
            }
        }

        override fun update(v: ItemMusicResBinding, item: MusicRes, position: Int) {
            if (item.isBasic) v.delete.pureColor = 0
            else v.delete.load(R.drawable.icon_delete_blue)
            v.description.text = item.description
            v.fs.text = item.fileSize
            v.name.text = item.name
            v.root.backgroundColor = main.rc(item.color).attachAlpha(180)
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
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                v.name.text = it
                musicInfo.name = it
                main.sendBottomMessage(FragmentLibrary::class, RachelMessage.LIBRARY_UPDATE_MUSIC_INFO, musicInfo)
                musicInfo.rewrite()
            }
        }

        v.singer.rachelClick {
            RachelDialog.input(main, "修改演唱歌手", 32) {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                v.singer.text = it
                musicInfo.singer = it
                musicInfo.rewrite()
            }
        }

        v.lyricist.rachelClick {
            RachelDialog.input(main, "修改作词", 32) {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                v.lyricist.text = it
                musicInfo.lyricist = it
                musicInfo.rewrite()
            }
        }

        v.composer.rachelClick {
            RachelDialog.input(main, "修改作曲", 32) {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                v.composer.text = it
                musicInfo.composer = it
                musicInfo.rewrite()
            }
        }

        v.album.rachelClick {
            RachelDialog.input(main, "修改专辑分类", 32) {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                v.album.text = it
                musicInfo.album = it
                musicInfo.rewrite()
            }
        }

        v.name.text = musicInfo.name
        v.version.text = musicInfo.version
        v.id.text = "ID: ${musicInfo.id}"
        v.author.text = "MOD来源: ${musicInfo.author}"
        v.pic.load(musicInfo.recordPath)
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
            GROUP_ADD -> {

            }
            GROUP_SHARE -> RachelDialog.confirm(main, content="导出MOD\"${this.id}\"到文件分享?") {
                shareMusic(musicInfo.id)
            }
        } }

        lifecycleScope.launch {
            v.loadingLyrics.loading = true
            if (musicInfo.lrcData == null) musicInfo.lrcData = withContext(Dispatchers.IO) { LrcData.parseLrcData(musicInfo.defaultLrcPath.readText()) }
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

    override fun back() = true

    private fun deleteRes(res: MusicRes) {
        when (res.id) {
            MusicRes.Type.VIDEO -> {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                val result = musicInfo.videoPath.delete()
                if (result) {
                    musicInfo.video = false
                    musicInfo.rewrite()
                }
            }
            MusicRes.Type.BGD -> {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                val result = musicInfo.bgdPath.delete()
                if (result) {
                    musicInfo.bgd = false
                    musicInfo.rewrite()
                }
            }
            MusicRes.Type.LRC -> {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                val lrcList = musicInfo.lyrics["line"]
                if (lrcList != null) {
                    val resId = res.name.removeSuffix(res.ext)
                    if (resId.isNotEmpty() && lrcList.contains(resId)) {
                        if (musicInfo.customPath(res.name).delete() && lrcList.remove(resId)) {
                            musicInfo.rewrite()
                        }
                    }
                }
            }
            MusicRes.Type.PAG -> {
                main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)
                val pagList = musicInfo.lyrics["pag"]
                if (pagList != null) {
                    val resId = res.name.removeSuffix(res.ext)
                    if (resId.isNotEmpty() && pagList.contains(resId)) {
                        if (musicInfo.customPath(res.name).delete() && pagList.remove(resId)) {
                            if (pagList.isEmpty()) musicInfo.lyrics.remove("pag")
                            musicInfo.rewrite()
                        }
                    }
                }
            }
            else -> { }
        }
    }

    // 分享歌曲
    @NewThread
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
                }
                catch (_: Exception) { null }
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