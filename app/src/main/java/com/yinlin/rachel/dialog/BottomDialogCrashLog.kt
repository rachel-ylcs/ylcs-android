package com.yinlin.rachel.dialog

import com.yinlin.rachel.RachelApplication
import com.yinlin.rachel.databinding.BottomDialogCrashLogBinding
import com.yinlin.rachel.fragment.FragmentSettings
import com.yinlin.rachel.interceptScroll
import com.yinlin.rachel.model.RachelBottomDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BottomDialogCrashLog(fragment: FragmentSettings) : RachelBottomDialog<BottomDialogCrashLogBinding, FragmentSettings>(
    fragment, 0.9f, BottomDialogCrashLogBinding::class.java) {

    override fun init() {
        v.log.interceptScroll()
    }

    fun update(): BottomDialogCrashLog {
        lifecycleScope.launch {
            v.root.smoothScrollTo(0, 0)
            v.log.text = withContext(Dispatchers.IO) { RachelApplication.crashHandler.getCrashLog() }
        }
        return this
    }
}