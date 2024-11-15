package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.yinlin.rachel.R
import com.yinlin.rachel.baseLine
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.textHeight

class ValueTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private var mValue: String
    private val mTitle: String
    private val mValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        typeface = context.resources.getFont(R.font.xwwk)
    }
    private val mTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = context.resources.getFont(R.font.xwwk)
    }
    private val mGap: Int
    private val mPadding: Int

    init {
        RachelAttr(context, attrs, R.styleable.ValueTextView).use {
            val textSize = it.spx(R.styleable.ValueTextView_android_textSize, R.dimen.sm)
            mValue = it.value(R.styleable.ValueTextView_Value, "")
            mTitle = it.value(R.styleable.ValueTextView_Title, "")
            mValuePaint.color = it.color(R.styleable.ValueTextView_ValueColor, R.color.black)
            mTitlePaint.color = it.color(R.styleable.ValueTextView_TitleColor, R.color.black)
            mValuePaint.textSize = textSize * 1.2f
            mTitlePaint.textSize = textSize
            mGap = it.dp(R.styleable.ValueTextView_Gap, 5)
            mPadding = it.dp(R.styleable.ValueTextView_android_padding, 5)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val valueWidth = mValuePaint.measureText(mValue).toInt()
        val titleWidth = mTitlePaint.measureText(mTitle).toInt()
        val w = valueWidth.coerceAtLeast(titleWidth) + 2 * mPadding
        val h = (mValuePaint.textHeight + mTitlePaint.textHeight + mGap).toInt() + 2 * mPadding
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val y1 = mValuePaint.textHeight - mValuePaint.baseLine + mPadding
        val y2 = height - mTitlePaint.baseLine - mPadding
        canvas.drawText(mValue, width * 0.5f, y1, mValuePaint)
        canvas.drawText(mTitle, width * 0.5f, y2, mTitlePaint)
    }

    var text: String
        get() = mValue
        set(v) {
            mValue = v
            requestLayout()
            invalidate()
        }
}