package com.yinlin.rachel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.tencent.mmkv.MMKV
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale


class RachelApplication : Application() {
    companion object {
        fun initBaseContext(context: Context): Context =
            context.createConfigurationContext(context.resources.configuration.apply {
            fontScale = 1f
            densityDpi = 480
            setLocale(Locale.SIMPLIFIED_CHINESE)
        })
    }

    class CrashHandler(val context: Context) : Thread.UncaughtExceptionHandler {
        private val sw = StringWriter()
        private val pw = PrintWriter(sw)

        override fun uncaughtException(t: Thread, e: Throwable) {
            e.printStackTrace(pw)
            context.contentResolver.apply {
                val uri = insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "rachel-crash.txt")
                })!!
                openOutputStream(uri).use { it?.write(sw.toString().toByteArray()) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 崩溃记录
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        // 初始化目录
        basePath = filesDir.path
        pathAPP.createAll()
        pathMusic.createAll()

        // 初始化MMKV
        MMKV.initialize(this)
        Config.kv = MMKV.defaultMMKV()

        // 补丁
    }

    override fun attachBaseContext(base: Context) = super.attachBaseContext(initBaseContext(base))
}