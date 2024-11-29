package com.yinlin.rachel.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.textSizePx
import com.yinlin.rachel.tool.visible

class LoadingTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {
    private val mTextView = TextView(context)
    private val mProgress = ProgressBar(context)
    private var mGap: Int
    private var mGravity: Int
    private var mLoading: Boolean
    init {
        RachelAttr(context, attrs, R.styleable.LoadingTextView).use {
            mTextView.id = View.generateViewId()
            mProgress.id = View.generateViewId()

            mGap = it.dp(R.styleable.LoadingTextView_Gap, 10)
            mGravity = it.value(R.styleable.LoadingTextView_android_gravity, Gravity.START)
            mLoading = it.value(R.styleable.LoadingTextView_Loading, false)
            mTextView.setTextColor(it.textColor(R.styleable.LoadingTextView_android_textColor))
            mTextView.textSizePx = it.spx(R.styleable.ValueTextView_android_textSize, R.dimen.sm)
            mTextView.text = it.value(R.styleable.LoadingTextView_android_text, "")
            mTextView.paint.isFakeBoldText = it.value(R.styleable.LoadingTextView_Bold, true)
            processLoading()
            processGravity()
        }
        addView(mTextView)
        addView(mProgress)
    }

    var textColorList: ColorStateList
        get() = mTextView.textColors
        set(value) { mTextView.setTextColor(value) }

    var textColor: Int
        get() = mTextView.textColor
        set(value) { mTextView.textColor = value }

    var textSize: Float
        get() = mTextView.textSize
        set(value) { mTextView.textSize = value }

    var textSizePx: Float
        get() = mTextView.textSizePx
        set(value) { mTextView.textSizePx = value }

    var text: String
        get() = mTextView.text.toString()
        set(value) { mTextView.text = value }

    var bold: Boolean
        get() = mTextView.paint.isFakeBoldText
        set(value) { mTextView.paint.isFakeBoldText = value }

    var gap: Int
        get() = mGap
        set(value) {
            mGap = value
            if (text.isNotEmpty()) {
                val textParams = mTextView.layoutParams as MarginLayoutParams
                textParams.marginEnd = value
                mTextView.layoutParams = textParams
                val progressParams = mProgress.layoutParams as MarginLayoutParams
                progressParams.marginStart = value
                mProgress.layoutParams = progressParams
            }
        }

    private fun processLoading() {
        if (loading) {
            visible = true
            mProgress.visible = true
        }
        else {
            mProgress.visible = false
            if (text.isEmpty()) visible = false
        }
    }

    var loading: Boolean
        get() = mLoading
        set(value) {
            mLoading = value
            processLoading()
        }

    private fun processGravity() {
        mTextView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            if (text.isNotEmpty()) setMargins(0, 0, mGap, 0)
            topToTop = 0
            if ((mGravity and Gravity.START) == Gravity.START) leftToLeft = 0
            else if ((mGravity and Gravity.END) == Gravity.END) rightToLeft = mProgress.id
            else {
                leftToLeft = 0
                rightToLeft = mProgress.id
                horizontalChainStyle = 2
            }
        }
        mProgress.layoutParams = LayoutParams(0, 0).apply {
            if (text.isNotEmpty()) setMargins(mGap, 0, 0, 0)
            topToTop = mTextView.id
            bottomToBottom = mTextView.id
            dimensionRatio = "1:1"
            if ((mGravity and Gravity.START) == Gravity.START) leftToRight = mTextView.id
            else if ((mGravity and Gravity.END) == Gravity.END) rightToRight = 0
            else {
                leftToRight = mTextView.id
                rightToRight = 0
                horizontalChainStyle = 2
            }
        }
    }

    var gravity: Int
        get() = mGravity
        set(value) {
            mGravity = value
            processGravity()
        }
}