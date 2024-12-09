package com.yinlin.rachel.tool

import com.yinlin.rachel.tool.Config.kv

object Patch {
    suspend fun inject(name: String, patch: suspend () -> Unit) {
        val patchName = "patch_${name}"
        if (!kv.containsKey(patchName)) {
            kv.encode(patchName, true)
            patch()
        }
    }
}