package com.yinlin.rachel.view


import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.textColor
import kotlin.math.max

class TagView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ViewGroup(context, attrs, defStyleAttr) {
    private val mTextViewList = mutableListOf<TextView>()
    private val mTextColor: Int
    private val mItemPaddingX: Int
    private val mItemPaddingY: Int
    private val mGap: Int

    var listener: (Int, String) -> Unit = { _, _ -> }

    init {
        RachelAttr(context, attrs, R.styleable.TagView).use {
            mTextColor = it.color(R.styleable.TagView_android_textColor, R.color.black)
            mItemPaddingX = it.dp(R.styleable.TagView_ItemPaddingX, 10)
            mItemPaddingY = it.dp(R.styleable.TagView_ItemPaddingY, 6)
            mGap = it.dp(R.styleable.TagView_Gap, 8)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val unspecifiedMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        var widthSum = 0
        var totalHeight = 0
        var heightMax = 0
        for (view in mTextViewList) {
            view.measure(unspecifiedMeasureSpec, unspecifiedMeasureSpec)
            if (view.measuredWidth + widthSum > widthSize) {
                widthSum = 0
                totalHeight += heightMax + mGap
                heightMax = 0
            }
            widthSum += view.measuredWidth + mGap
            heightMax = max(heightMax, view.measuredHeight)
        }
        totalHeight += heightMax + paddingTop + paddingBottom
        val totalHeightMeasureSpec = MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY)
        setMeasuredDimension(widthMeasureSpec, totalHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        var widthSum = 0
        var heightMax = 0
        var currentTop = paddingTop
        var currentLeft = paddingLeft
        for (view in mTextViewList) {
            if (view.measuredWidth + widthSum > width) {
                widthSum = 0
                currentLeft = paddingLeft
                currentTop += heightMax + mGap
                heightMax = 0
            }
            view.layout(currentLeft, currentTop, currentLeft + view.measuredWidth, currentTop + view.measuredHeight)
            currentLeft += view.measuredWidth + mGap
            widthSum += view.measuredWidth + mGap
            heightMax = max(heightMax, view.measuredHeight)
        }
    }

    private fun addTagImpl(tag: String) {
        val textView = TextView(context).apply {
            text = tag
            textColor = mTextColor
            gravity = Gravity.CENTER
            setPadding(mItemPaddingX, mItemPaddingY, mItemPaddingX, mItemPaddingY)
            setBackgroundResource(R.drawable.bg_tag)
            rachelClick { listener(mTextViewList.indexOf(this), text.toString()) }
        }
        mTextViewList.add(textView)
        addView(textView)
    }

    fun setTags(tags: List<String>) {
        removeAllViewsInLayout()
        mTextViewList.clear()
        for (tag in tags) addTagImpl(tag)
        requestLayout()
    }

    fun addTag(tag: String) {
        addTagImpl(tag)
        requestLayout()
    }

    fun removeTag(index: Int) {
        if (index in 0..< mTextViewList.size) {
            mTextViewList.removeAt(index)
            removeViewAt(index)
            requestLayout()
        }
    }
}