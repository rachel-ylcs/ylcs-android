package com.yinlin.rachel.model

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.annotation.AnyRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StyleableRes
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rd
import com.yinlin.rachel.tool.toDP

class RachelAttr(private val context: Context, attrs: AttributeSet?, @StyleableRes ids: IntArray) : AutoCloseable {
    val attr: TypedArray? = attrs?.let { context.obtainStyledAttributes(attrs, ids) }

    inline fun <reified T> value(@StyleableRes id: Int, default: T): T {
        return attr?.let {
            when (default) {
                is Boolean -> it.getBoolean(id, default) as T
                is Int -> it.getInt(id, default) as T
                is Float -> it.getFloat(id, default) as T
                is String -> (it.getString(id) ?: default) as T
                else -> default
            }
        } ?: default
    }

    @ColorInt
    fun color(@StyleableRes id: Int, @ColorRes default: Int): Int {
        val value = context.rc(default)
        return attr?.getColor(id, value) ?: value
    }

    fun colorList(@StyleableRes id: Int, default: ColorStateList): ColorStateList {
        val resId = attr?.getResourceId(id, -1) ?: -1
        if (resId != -1) {
            val colors = context.getColorStateList(resId)
            if (colors.isStateful) return colors
        }
        return default
    }

    fun dp(@StyleableRes id: Int, default: Int): Int {
        val value = if (default <= 0) default else default.toDP(context)
        return attr?.getDimensionPixelSize(id, value) ?: value
    }

    fun dp(@StyleableRes id: Int, default: Float): Float {
        val value = if (default <= 0) default else default.toDP(context)
        return attr?.getDimensionPixelSize(id, value.toInt())?.toFloat() ?: value
    }

    fun spx(@StyleableRes id: Int, @DimenRes default: Int): Float {
        val value = context.rd(default)
        return (attr?.getDimensionPixelSize(id, value) ?: value).toFloat()
    }

    inline fun src(@StyleableRes id: Int, ref: (Int) -> Unit, color: (Int) -> Unit) {
        val resId = attr?.getResourceId(id, -1) ?: -1
        if (resId != -1) ref(resId)
        else color(attr?.getColor(id, 0) ?: 0)
    }

    fun ref(@StyleableRes id: Int, @AnyRes default: Int = -1) = attr?.getResourceId(id, default) ?: default

    fun textColor(@StyleableRes id: Int) = try {
        val resId = attr?.getResourceId(id, -1) ?: -1
        if (resId == -1) throw Exception()
        context.getColorStateList(resId)
    }
    catch (_: Exception) { ColorStateList.valueOf(context.rc(R.color.steel_blue)) }

    override fun close() { attr?.recycle() }
}