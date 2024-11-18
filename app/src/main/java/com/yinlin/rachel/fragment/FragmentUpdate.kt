package com.yinlin.rachel.fragment

import android.content.Intent
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.Net
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.sys.DevelopState
import com.yinlin.rachel.databinding.FragmentUpdateBinding
import com.yinlin.rachel.databinding.ItemDevelopStateBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.textColor
import com.yinlin.rachel.tip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.sangcomz.stickytimelineview.callback.SectionCallback
import xyz.sangcomz.stickytimelineview.model.SectionInfo


class FragmentUpdate(main: MainActivity) : RachelFragment<FragmentUpdateBinding>(main) {
    class Adapter(private val main: MainActivity) : RachelAdapter<ItemDevelopStateBinding, DevelopState>() {
        override fun bindingClass() = ItemDevelopStateBinding::class.java

        override fun update(v: ItemDevelopStateBinding, item: DevelopState, position: Int) {
            v.content.text = item.content
            v.content.textColor = main.rc(item.color)
        }
    }

    override fun bindingClass() = FragmentUpdateBinding::class.java

    private val adapter = Adapter(main)
    private var downloadUrl: String? = null
    private var isNeedUpdate: Boolean = false

    override fun init() {
        v.targetVersion.rachelClick {
            if (downloadUrl == null) {
                tip(Tip.WARNING, "未获取到服务器最新安装包")
                return@rachelClick
            }
            if (!isNeedUpdate) {
                tip(Tip.SUCCESS, "当前已经是最新版本")
                return@rachelClick
            }
            Net.downloadFile(main, downloadUrl!!, object : Net.DownLoadMediaListener {
                override fun onCancel() { }
                override fun onDownloadComplete(status: Boolean, uri: Uri?) {
                    try {
                        if (status && uri != null) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "application/vnd.android.package-archive")
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            return
                        }
                    }
                    catch (ignored: Exception) { }
                    tip(Tip.ERROR, "下载失败")
                }
            })
        }

        v.list.apply {
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 10)
            layoutManager = LinearLayoutManager(main, LinearLayoutManager.VERTICAL, false)
            adapter = this@FragmentUpdate.adapter
        }

        checkUpdate()
    }

    override fun back() = true

    // 检查更新
    @NewThread
    private fun checkUpdate() {
        val appVersion = main.appVersion
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { API.CommonAPI.getServerInfo() }
            if (result.success) {
                val info = result.data
                downloadUrl = info.downloadUrl
                if (appVersion < info.targetVersion) isNeedUpdate = true
                v.appVersion.text = "${main.rs(R.string.app_name)} ${main.appVersionName(appVersion)}"
                v.targetVersion.text = "服务器版本: ${main.appVersionName(info.targetVersion)}"
                v.minVersion.text = "最低兼容版本: ${main.appVersionName(info.minVersion)}"
                v.appVersion.textColor = main.rc(
                    if (appVersion == info.targetVersion) R.color.sea_green
                    else if (appVersion >= info.minVersion) R.color.orange_red
                    else R.color.dark_red
                )
                v.list.addItemDecoration(object : SectionCallback {
                    override fun getSectionHeader(position: Int) = SectionInfo(adapter[position].name, "", AppCompatResources.getDrawable(main, adapter[position].icon))
                    override fun isSection(position: Int): Boolean = adapter[position].type != adapter[position - 1].type
                })
                adapter.setSource(info.developState)
                adapter.notifySource()
            }
            else {
                main.pop()
                tip(Tip.ERROR, result.msg)
            }
        }
    }
}