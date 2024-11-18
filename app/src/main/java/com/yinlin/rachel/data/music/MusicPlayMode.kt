package com.yinlin.rachel.data.music

import com.yinlin.rachel.model.RachelEnum

class MusicPlayMode(ordinal: Int) : RachelEnum(ordinal) {
    companion object {
        val ORDER = MusicPlayMode(0)
        val LOOP = MusicPlayMode(1)
        val RANDOM = MusicPlayMode(2)
    }
}