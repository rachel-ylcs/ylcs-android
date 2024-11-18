package com.yinlin.rachel.model

open class RachelEnum(val ordinal: Int) {
    override fun equals(other: Any?): Boolean {
        return if (other is RachelEnum) return ordinal == other.ordinal else false
    }
    override fun hashCode() = ordinal
}