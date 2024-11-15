package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr

class AspectRatioImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    private val ratio: Float

    init {
        RachelAttr(context, attrs, R.styleable.AspectRatioImageView).use {
            ratio = it.value(R.styleable.AspectRatioImageView_Ratio, 1f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var w = measuredWidth
        var h = measuredHeight
        when {
            w > 0 -> h = (w / ratio).toInt()
            h > 0 -> w = (h * ratio).toInt()
            else -> return
        }
        setMeasuredDimension(w, h)
    }
}