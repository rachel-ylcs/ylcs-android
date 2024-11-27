package com.yinlin.rachel.sheet

import com.yinlin.rachel.RachelApplication
import com.yinlin.rachel.databinding.SheetCrashLogBinding
import com.yinlin.rachel.fragment.FragmentSettings
import com.yinlin.rachel.interceptScroll
import com.yinlin.rachel.model.RachelSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SheetCrashLog(fragment: FragmentSettings) : RachelSheet<SheetCrashLogBinding, FragmentSettings>(fragment, 0.7f) {
    override fun bindingClass() = SheetCrashLogBinding::class.java

    override fun init() {
        v.log.interceptScroll()
        lifecycleScope.launch {
            v.log.text = withContext(Dispatchers.IO) { RachelApplication.crashHandler.getCrashLog() }
        }
    }
}