package com.yinlin.rachel.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap

fun Context.rs(@StringRes id: Int): String = this.getString(id)
@ColorInt fun Context.rc(@ColorRes id: Int): Int = this.getColor(id)
fun Context.rd(@DimenRes id: Int): Int = this.resources.getDimensionPixelSize(id)
fun Context.rf(@FontRes id: Int): Typeface = this.resources.getFont(id)
fun Context.ri(@DrawableRes id: Int): Drawable = AppCompatResources.getDrawable(this, id)!!
fun Context.rb(@DrawableRes id: Int): Bitmap = this.ri(id).toBitmap()
fun Context.rb(@DrawableRes id: Int, width: Int, height: Int) = this.ri(id).toBitmap(width, height)