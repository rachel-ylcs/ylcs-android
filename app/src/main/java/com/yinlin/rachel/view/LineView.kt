package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr

class LineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val paint = Paint()

    init {
        RachelAttr(context, attrs, R.styleable.LineView).use {
            paint.color = it.color(R.styleable.LineView_android_color, R.color.light_gray)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}