package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.baseLine
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rf
import com.yinlin.rachel.tool.textHeight
import com.yinlin.rachel.tool.toDP
import kotlin.math.min


class DiagonalLabelView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    companion object {
        const val LEFT_TOP = 1
        const val RIGHT_TOP = 2
        const val LEFT_BOTTOM = 3
        const val RIGHT_BOTTOM = 4
    }

    private val mRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.FILL
    }
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.SQUARE
        typeface = context.rf(R.font.xwwk)
    }
    private val mRectPath = Path().apply { reset() }
    private val mTextPath = Path().apply { reset() }
    private var mText: String
    private var mOrientation: Int

    init {
        RachelAttr(context, attrs, R.styleable.DiagonalLabelView).use {
            mText = it.value(R.styleable.DiagonalLabelView_android_text, "")
            mTextPaint.isFakeBoldText = it.value(R.styleable.DiagonalLabelView_Bold, false)
            mTextPaint.color = it.color(R.styleable.DiagonalLabelView_android_color, R.color.white)
            mRectPaint.color = it.color(R.styleable.DiagonalLabelView_BackgroundColor, R.color.steel_blue)
            mOrientation = it.value(R.styleable.DiagonalLabelView_DiagonalOrientation, LEFT_TOP)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) min(widthSize, heightSize)
        else 64.toDP(context)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mTextPaint.textSize = 1f
        var tw = mTextPaint.textHeight
        // 2 * sqrt(2) * 1.2
        while (tw < w / 3.3941126f) {
            mTextPaint.textSize += 1f
            tw = mTextPaint.textHeight
            if (mTextPaint.measureText(mText) > w / 1.2f) break
        }
        val shadowPixel = w / 100f
        mRectPaint.setShadowLayer(shadowPixel, shadowPixel, shadowPixel, context.rc(R.color.gray))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = measuredWidth.toFloat()
        when (mOrientation) {
            LEFT_TOP -> {
                mRectPath.apply {
                    reset()
                    moveTo(0f, s / 2)
                    lineTo(s / 2, 0f)
                    lineTo(s, 0f)
                    lineTo(0f, s)
                    close()
                }
                mTextPath.apply {
                    reset()
                    moveTo(0f, s * 0.75f)
                    lineTo(s * 0.75f, 0f)
                    close()
                }
            }
            RIGHT_TOP -> {
                mRectPath.apply {
                    reset()
                    moveTo(0f, 0f)
                    lineTo(s / 2, 0f)
                    lineTo(s, s / 2)
                    lineTo(s, s)
                    close()
                }
                mTextPath.apply {
                    reset()
                    moveTo(s * 0.25f, 0f)
                    lineTo(s, s * 0.75f)
                    close()
                }
            }
            LEFT_BOTTOM -> {
                mRectPath.apply {
                    reset()
                    moveTo(0f, 0f)
                    lineTo(s, s)
                    lineTo(s / 2, s)
                    lineTo(0f, s / 2)
                    close()
                }
                mTextPath.apply {
                    reset()
                    moveTo(0f, s * 0.25f)
                    lineTo(s * 0.75f, s)
                    close()
                }
            }
            RIGHT_BOTTOM -> {
                mRectPath.apply {
                    reset()
                    moveTo(0f, s)
                    lineTo(s, 0f)
                    lineTo(s, s / 2)
                    lineTo(s / 2, s)
                    close()
                }
                mTextPath.apply {
                    reset()
                    moveTo(s * 0.25f, s)
                    lineTo(s, s * 0.25f)
                    close()
                }
            }
        }
        canvas.drawPath(mRectPath, mRectPaint)
        // 0.75 / sqrt(2)
        val x = s * 0.53033f - mTextPaint.measureText(mText) / 2f
        val y = (mTextPaint.textHeight - mTextPaint.baseLine * 1.4142135f) / 2f
        canvas.drawTextOnPath(mText, mTextPath, x, y, mTextPaint)
    }

    var text: String
        get() = mText
        set(value) {
            mText = value
            invalidate()
        }

    var textColor: Int
        get() = mTextPaint.color
        set(value) {
            mTextPaint.color = value
            invalidate()
        }

    var bgColor: Int
        get() = mRectPaint.color
        set(value) {
            mRectPaint.color = value
            invalidate()
        }

    var bold: Boolean
        get() = mTextPaint.isFakeBoldText
        set(value) {
            mTextPaint.isFakeBoldText = value
            invalidate()
        }

    var orientation: Int
        get() = mOrientation
        set(value) {
            mOrientation = value
            invalidate()
        }
}