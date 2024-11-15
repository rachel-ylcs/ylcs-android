package com.yinlin.rachel.view

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.textSizePx
import com.yinlin.rachel.toDP

class InputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TextInputLayout(context, attrs, defStyleAttr), TextWatcher {
    fun interface OverFlowListener {
        fun onOverFlow(status: Boolean)
    }

    private var mMaxLines: Int
    private var mMaxLength: Int
    private val inputView: TextInputEditText = TextInputEditText(context)
    private var mOverflowListener: OverFlowListener? = null

    init {
        RachelAttr(context, attrs, R.styleable.InputView).use {
            mMaxLines = it.value(R.styleable.InputView_android_maxLines, 1).coerceAtLeast(1)
            mMaxLength = it.value(R.styleable.InputView_android_maxLength, 0).coerceAtLeast(0)
            inputView.apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                val padding = 10.toDP(context)
                setPadding(padding, padding, padding, padding)
                gravity = Gravity.START
                setRawInputType(it.value(R.styleable.InputView_android_inputType, InputType.TYPE_CLASS_TEXT))
                textSizePx = it.spx(R.styleable.InputView_android_textSize, R.dimen.sm)
                isSingleLine = mMaxLines == 1
                maxLines = mMaxLines
                setLines(maxLines)
                addTextChangedListener(this@InputView)
            }
        }

        counterMaxLength = mMaxLength
        isCounterEnabled = mMaxLength > 0
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addView(inputView, 0)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { }
    override fun afterTextChanged(e: Editable) = checkOverFlow(e.toString())

    private fun checkOverFlow(str: String) {
        if (mMaxLength > 0) overflowListener?.onOverFlow(str.let { !(it.isNotEmpty() && it.length <= mMaxLength) })
    }

    var text: String
        get() = inputView.text?.toString() ?: ""
        set(value) { inputView.setText(value) }

    var inputType: Int
        get() = inputView.inputType
        set(value) { inputView.inputType = value }

    var maxLines: Int
        get() = mMaxLines
        set(value) {
            mMaxLines = value
            inputView.apply {
                maxLines = mMaxLines
                setLines(maxLines)
            }
        }

    var maxLength: Int
        get() = mMaxLength
        set(value) {
            mMaxLength = value
            counterMaxLength = mMaxLength
            isCounterEnabled = mMaxLength > 0
            checkOverFlow(text)
        }

    var overflowListener: OverFlowListener?
        get() = mOverflowListener
        set(value) {
            mOverflowListener = value
            checkOverFlow(text)
        }
}