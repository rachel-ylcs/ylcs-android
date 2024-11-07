package com.yinlin.rachel.data.sys

import com.yinlin.rachel.R

data class DevelopState(val type: Type, val content: String) {
    enum class Type { NEW, ADJUSTMENT, REPAIR, WORKING, FEATURE, FUTURE }

    val icon: Int get() = when (type) {
        Type.NEW -> R.drawable.svg_develop_state_new
        Type.ADJUSTMENT -> R.drawable.svg_develop_state_adjustment
        Type.REPAIR -> R.drawable.svg_develop_state_repair
        Type.WORKING -> R.drawable.svg_develop_state_working
        Type.FEATURE -> R.drawable.svg_develop_state_feature
        Type.FUTURE -> R.drawable.svg_develop_state_future
    }

    val name: String get() = when (type) {
        Type.NEW -> "新增"
        Type.ADJUSTMENT -> "调整"
        Type.REPAIR -> "修复"
        Type.WORKING -> "进行中"
        Type.FEATURE -> "特性 - 众议"
        Type.FUTURE -> "计划中"
    }

    val color: Int get() = when (type) {
        Type.NEW -> R.color.steel_blue
        Type.ADJUSTMENT -> R.color.orange_red
        Type.REPAIR -> R.color.dark_red
        Type.WORKING -> R.color.sea_green
        Type.FEATURE -> R.color.pink
        Type.FUTURE -> R.color.purple
    }
}

typealias DevelopStateList = List<DevelopState>