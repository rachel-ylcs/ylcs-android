package com.yinlin.rachel.data.sys

import com.yinlin.rachel.R

data class DevelopState(val type: Int, val content: String) {
    companion object {
        const val NEW = 0
        const val ADJUSTMENT = 1
        const val REPAIR = 2
        const val WORKING = 3
        const val FEATURE = 4
        const val FUTURE = 5
    }

    val icon: Int get() = when (type) {
        NEW -> R.drawable.icon_develop_state_new
        ADJUSTMENT -> R.drawable.icon_develop_state_adjustment
        REPAIR -> R.drawable.icon_develop_state_repair
        WORKING -> R.drawable.icon_develop_state_working
        FEATURE -> R.drawable.icon_develop_state_feature
        FUTURE -> R.drawable.icon_develop_state_future
        else -> R.drawable.icon_develop_state_new
    }

    val name: String get() = when (type) {
        NEW -> "新增"
        ADJUSTMENT -> "调整"
        REPAIR -> "修复"
        WORKING -> "进行中"
        FEATURE -> "特性 - 众议"
        FUTURE -> "计划中"
        else -> "未知"
    }

    val color: Int get() = when (type) {
        NEW -> R.color.steel_blue
        ADJUSTMENT -> R.color.orange_red
        REPAIR -> R.color.dark_red
        WORKING -> R.color.sea_green
        FEATURE -> R.color.pink
        FUTURE -> R.color.purple
        else -> R.color.steel_blue
    }
}

typealias DevelopStateList = List<DevelopState>