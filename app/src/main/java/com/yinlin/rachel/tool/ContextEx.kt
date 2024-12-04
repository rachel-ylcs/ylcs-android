package com.yinlin.rachel.tool

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.FontRes
import androidx.annotation.StringRes

fun Context.rs(@StringRes id: Int): String = this.getString(id)
@ColorInt fun Context.rc(@ColorRes id: Int): Int = this.getColor(id)
fun Context.rd(@DimenRes id: Int): Int = this.resources.getDimensionPixelSize(id)
fun Context.rf(@FontRes id: Int): Typeface = this.resources.getFont(id)