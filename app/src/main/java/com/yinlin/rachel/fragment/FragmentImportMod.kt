package com.yinlin.rachel.fragment

import android.net.Uri
import android.widget.SimpleAdapter
import androidx.annotation.ColorRes
import androidx.lifecycle.lifecycleScope
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.mod.Metadata
import com.yinlin.rachel.data.music.PlayingMusicPreviewList
import com.yinlin.rachel.tool.backgroundColor
import com.yinlin.rachel.databinding.FragmentImportModBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelMod
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.pathMusic
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.rc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FragmentImportMod(main: MainActivity, private val uri: Uri) : RachelFragment<FragmentImportModBinding>(main) {
    private var canCancel = true
    private var releaser: RachelMod.Releaser? = null

    override fun bindingClass() = FragmentImportModBinding::class.java

    @IOThread
    override fun init() {
        var errorText = "导入失败"
        try {
            val releaser = RachelMod.Releaser(main.contentResolver.openInputStream(uri)!!)
            this.releaser = releaser
            val metadata = releaser.getMetadata()
            if (metadata.empty || metadata.version != RachelMod.MOD_VERSION) {
                errorText = "Mod版本不一致"
                throw Exception()
            }
            val ids = mutableListOf<String>()
            val listData = mutableListOf<Map<String, String>>()
            for ((id, value) in metadata.items) {
                listData += hashMapOf("line1" to "${value.name} - $id", "line2" to "version ${value.version}")
                ids.add(id)
            }
            v.list.adapter = SimpleAdapter(
                main, listData, android.R.layout.simple_list_item_2,
                arrayOf("line1", "line2"), intArrayOf(android.R.id.text1, android.R.id.text2)
            )
            val totalNum = metadata.totalCount
            v.tvTotalNum.text = totalNum.toString()
            v.buttonOk.rachelClick { importMod(metadata, totalNum, ids) }
        } catch (e: Exception) {
            releaser?.close()
            setButtonStatus(errorText, R.color.red)
        }
    }

    override fun quit() {
        releaser?.close()
    }

    override fun back() = if (canCancel) BackState.POP else BackState.HOME

    private fun setButtonStatus(text: String, @ColorRes color: Int) {
        v.buttonOk.apply {
            isEnabled = false
            backgroundColor = main.rc(color)
            this.text = text
        }
    }

    @IOThread
    private fun importMod(metadata: Metadata, totalNum: Int, ids: List<String>) {
        releaser?.let {
            lifecycleScope.launch {
                // 如果导入的歌曲正在播放则只能停止播放器
                val playlist = main.sendMessageForResult<PlayingMusicPreviewList>(RachelTab.music, RachelMessage.MUSIC_GET_CURRENT_PLAYLIST_PREVIEW)!!
                var needStopPlayer = false
                for (id in ids) {
                    if (playlist.indexOfFirst { it.id == id } != -1) {
                        needStopPlayer = true
                        break
                    }
                }
                if (needStopPlayer) main.sendMessage(RachelTab.music, RachelMessage.MUSIC_STOP_PLAYER)

                setButtonStatus("导入中...", R.color.orange)
                canCancel = false
                var runResult = false
                withContext(Dispatchers.IO) {
                    try {
                        runResult = it.run(pathMusic) { id, res, index -> post {
                            val curIndex = index + 1
                            val percent = curIndex * 100 / totalNum
                            v.tvRes.text = "导入: ${metadata.items[id]!!.name}$res"
                            v.tvPercent.text = "$percent %"
                            v.tvCurNum.text = curIndex.toString()
                            v.progress.progress = percent
                        } }
                        it.close()
                    }
                    catch (_: Exception) { }
                    canCancel = true
                }
                if (runResult) {
                    setButtonStatus("导入成功", R.color.green)
                    // 提醒播放器更新
                    main.sendMessage(RachelTab.music, RachelMessage.MUSIC_NOTIFY_ADD_MUSIC, ids)
                }
                else setButtonStatus("导入失败", R.color.red)
            }
        }
    }
}