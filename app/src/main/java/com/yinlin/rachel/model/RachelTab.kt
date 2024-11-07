package com.yinlin.rachel.model

import androidx.annotation.DrawableRes
import com.yinlin.rachel.R
import com.yinlin.rachel.fragment.FragmentDiscovery
import com.yinlin.rachel.fragment.FragmentMe
import com.yinlin.rachel.fragment.FragmentMsg
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.fragment.FragmentWorld
import kotlin.random.Random

data class RachelTab(
    val index: Int,
    val prototype: Class<out RachelFragment<*>>,
    val title: String,
    @DrawableRes val iconNormal: Int,
    @DrawableRes val iconActive: Int
) {
    companion object {
        private val isLuckyDog: Boolean = Random.nextInt(100) < 1

        val msg = RachelTab(0, FragmentMsg::class.java, "资讯", R.drawable.msg_normal, R.drawable.msg_active)
        val discovery = RachelTab(1, FragmentDiscovery::class.java, "发现", R.drawable.discovery_normal, R.drawable.discovery_active)
        val music = RachelTab(2, FragmentMusic::class.java, "听歌", R.drawable.music_normal, R.drawable.music_active)
        val world = RachelTab(3, FragmentWorld::class.java, "世界", R.drawable.world_normal, R.drawable.world_active)
        val me = RachelTab(4, FragmentMe::class.java, "小银子", if (isLuckyDog) R.drawable.dog_normal else R.drawable.me_normal, if (isLuckyDog) R.drawable.dog_active else R.drawable.me_active)
    }
}