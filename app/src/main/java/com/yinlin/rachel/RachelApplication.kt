package com.yinlin.rachel

import android.app.Application
import android.content.Context
import android.text.format.DateFormat
import com.tencent.mmkv.MMKV
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RachelApplication : Application() {
    class CrashHandler : Thread.UncaughtExceptionHandler {
        private val sw = StringWriter()
        private val pw = PrintWriter(sw)
        private val crashFile = pathAPP / "rachel-crash.txt"

        override fun uncaughtException(t: Thread, e: Throwable) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            pw.println("${dateFormat.format(Date())}\n")
            e.printStackTrace(pw)
            crashFile.writeText(sw.toString())
        }

        fun getCrashLog(): String = crashFile.readText()
    }

    companion object {
        fun initBaseContext(context: Context): Context =
            context.createConfigurationContext(context.resources.configuration.apply {
                fontScale = 1f
                densityDpi = if (densityDpi >= 480) 480 else 320
                setLocale(Locale.SIMPLIFIED_CHINESE)
            })
        lateinit var crashHandler: CrashHandler
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化目录
        basePath = filesDir.path
        pathAPP.createAll()
        pathMusic.createAll()

        // 崩溃记录
        crashHandler = CrashHandler()
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)

        // 初始化MMKV
        MMKV.initialize(this)
        Config.kv = MMKV.defaultMMKV()

        // 补丁
    }

    override fun attachBaseContext(base: Context) = super.attachBaseContext(initBaseContext(base))
}