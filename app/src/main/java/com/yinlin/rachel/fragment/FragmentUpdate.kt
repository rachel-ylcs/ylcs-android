package com.yinlin.rachel.fragment

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Net
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.api.API
import com.yinlin.rachel.common.DialogMediaDownloadListener
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.sys.DevelopState
import com.yinlin.rachel.databinding.FragmentUpdateBinding
import com.yinlin.rachel.databinding.ItemDevelopStateBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rs
import com.yinlin.rachel.tool.startIO
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.withMain

@Layout(FragmentUpdateBinding::class)
class FragmentUpdate(main: MainActivity) : RachelFragment<FragmentUpdateBinding>(main) {
    @Layout(ItemDevelopStateBinding::class)
    class Adapter : RachelAdapter<ItemDevelopStateBinding, DevelopState>() {
        override fun update(v: ItemDevelopStateBinding, item: DevelopState, position: Int) {
            v.content.text = item.content
        }
    }

    class Manager(context: Context?): LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
        override fun canScrollVertically() = false
    }

    private var downloadUrl: String? = null
    private var isNeedUpdate: Boolean = false

    private lateinit var lists: List<RecyclerView>

    override fun init() {
        v.targetVersion.rachelClick {
            val url = downloadUrl
            if (url == null) tip(Tip.WARNING, "未获取到服务器最新安装包")
            else if (!isNeedUpdate) tip(Tip.SUCCESS, "当前已经是最新版本")
            else downloadAPK(url)
        }
        lists = listOf(v.listNew, v.listAdjustment, v.listRepair, v.listWorking, v.listFeature, v.listFuture)
        for (list in lists) {
            list.layoutManager = Manager(context)
            list.adapter = Adapter()
        }

        checkUpdate()
    }

    override fun back() = BackState.POP

    // 检查更新
    @IOThread
    private fun checkUpdate() {
        val appVersion = main.appVersion
        startIOWithResult({ API.CommonAPI.getServerInfo() }) {
            if (it.success) {
                val info = it.data
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
                for (state in info.developState) (lists.getOrNull(state.type)?.adapter as? Adapter?)?.items?.add(state)
                for (list in lists) (list.adapter as Adapter).notifySource()
            }
            else {
                main.pop()
                tip(Tip.ERROR, it.msg)
            }
        }
    }

    // 下载最新安装包
    @IOThread
    private fun downloadAPK(url: String) {
        startIO {
            Net.download(url, listener = object : DialogMediaDownloadListener(main) {
                override fun makeMediaUri(url: String, values: ContentValues): Uri {
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, url.substringAfterLast('/'))
                    values.put(MediaStore.Images.Media.MIME_TYPE, "application/vnd.android.package-archive")
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    return MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                override suspend fun onCompleted() {
                    withMain {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "application/vnd.android.package-archive")
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                        catch (_: Exception) {
                            tip(Tip.ERROR, "打开安装包失败, 请手动在下载目录安装")
                        }
                    }
                }

                override suspend fun onFailed() {
                    withMain { tip(Tip.ERROR, "下载失败") }
                }
            })
        }
    }
}