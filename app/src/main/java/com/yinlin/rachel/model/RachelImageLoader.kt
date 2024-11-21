package com.yinlin.rachel.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import java.io.File

object RachelImageLoader {
    private val resOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.NONE)

    private val fileOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)

    private val netOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.placeholder_pic)

    private val loadingOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.placeholder_loading)

    private val blackOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(ColorDrawable(Color.BLACK))

    fun ImageView.load(@RawRes @DrawableRes resourceId: Int) {
        Glide.with(this).load(resourceId).apply(resOptions).into(this)
    }

    fun ImageView.load(file: File) {
        Glide.with(this).load(file).apply(fileOptions).into(this)
    }

    fun ImageView.load(path: String) {
        Glide.with(this).load(path).apply(netOptions).into(this)
    }

    fun ImageView.load(path: String, key: Any) {
        Glide.with(this).load(path).apply(netOptions).signature(ObjectKey(key)).into(this)
    }

    fun ImageView.loadLoading(path: String) {
        Glide.with(this).load(path).apply(loadingOptions).into(this)
    }

    fun ImageView.loadBlack(path: String) {
        Glide.with(this).load(path).apply(blackOptions).into(this)
    }

    fun ImageView.loadDaily(path: String) {
        Glide.with(this).load(path).apply(netOptions).signature(ObjectKey(Config.cache_daily_pic)).into(this)
    }
}