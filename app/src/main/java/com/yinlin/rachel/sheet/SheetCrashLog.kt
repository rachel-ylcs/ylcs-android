package com.yinlin.rachel.sheet

import com.yinlin.rachel.RachelApplication
import com.yinlin.rachel.annotation.SheetLayout
import com.yinlin.rachel.databinding.SheetCrashLogBinding
import com.yinlin.rachel.fragment.FragmentSettings
import com.yinlin.rachel.tool.interceptScroll
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.tool.startIOWithResult

@SheetLayout(SheetCrashLogBinding::class, 0.7f)
class SheetCrashLog(fragment: FragmentSettings) : RachelSheet<SheetCrashLogBinding, FragmentSettings>(fragment) {
    override fun init() {
        v.log.interceptScroll()
        startIOWithResult({ RachelApplication.crashHandler.getCrashLog() }) {
            v.log.text = it
        }
    }
}