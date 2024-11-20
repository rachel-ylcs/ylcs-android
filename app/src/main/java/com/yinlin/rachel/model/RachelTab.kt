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

        val world = RachelTab(0, FragmentWorld::class.java, "世界", R.drawable.tab_world_normal, R.drawable.tab_world_active)
        val msg = RachelTab(1, FragmentMsg::class.java, "资讯", R.drawable.tab_msg_normal, R.drawable.tab_msg_active)
        val music = RachelTab(2, FragmentMusic::class.java, "听歌", R.drawable.tab_music_normal, R.drawable.tab_music_active)
        val discovery = RachelTab(3, FragmentDiscovery::class.java, "发现", R.drawable.tab_discovery_normal, R.drawable.tab_discovery_active)
        val me = RachelTab(4, FragmentMe::class.java, "小银子", if (isLuckyDog) R.drawable.tab_dog_normal else R.drawable.tab_me_normal, if (isLuckyDog) R.drawable.tab_dog_active else R.drawable.tab_me_active)
    }
}