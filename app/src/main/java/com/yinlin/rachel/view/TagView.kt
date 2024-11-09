package com.yinlin.rachel.view


import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.yinlin.rachel.R
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.textColor
import com.yinlin.rachel.toDP
import kotlin.math.max

class TagView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ViewGroup(context, attrs, defStyleAttr) {
    private val mTextViewList = mutableListOf<TextView>()
    private val mTextColor: Int
    private val mHorizontalPadding: Int
    private val mVerticalPadding: Int
    private val mTagMargin: Int

    var listener: (Int, String) -> Unit = { _, _ -> }

    init {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.TagView)
        mTextColor = attr.getColor(R.styleable.TagView_textColor, context.getColor(R.color.black))
        mHorizontalPadding = attr.getDimensionPixelSize(R.styleable.TagView_horizontalPadding, 10.toDP(context))
        mVerticalPadding = attr.getDimensionPixelSize(R.styleable.TagView_verticalPadding, 6.toDP(context))
        mTagMargin = attr.getDimensionPixelSize(R.styleable.TagView_tagMargin, 8.toDP(context))
        attr.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val unspecifiedMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        var widthSum = 0
        var totalHeight = 0
        var heightMax = 0
        for (view in mTextViewList) {
            view.measure(unspecifiedMeasureSpec, unspecifiedMeasureSpec)
            if (view.measuredWidth + widthSum > widthSize) {
                widthSum = 0
                totalHeight += heightMax + mTagMargin
                heightMax = 0
            }
            widthSum += view.measuredWidth + mTagMargin
            heightMax = max(heightMax, view.measuredHeight)
        }
        totalHeight += heightMax
        val totalHeightMeasureSpec = MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY)
        setMeasuredDimension(widthMeasureSpec, totalHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        var widthSum = 0
        var heightMax = 0
        var currentTop = 0
        var currentLeft = 0
        for (view in mTextViewList) {
            if (view.measuredWidth + widthSum > width) {
                widthSum = 0
                currentLeft = 0
                currentTop += heightMax + mTagMargin
                heightMax = 0
            }
            view.layout(currentLeft, currentTop, currentLeft + view.measuredWidth, currentTop + view.measuredHeight)
            currentLeft += view.measuredWidth + mTagMargin
            widthSum += view.measuredWidth + mTagMargin
            heightMax = max(heightMax, view.measuredHeight)
        }
    }

    private fun addTagImpl(tag: String) {
        val textView = TextView(context).apply {
            text = tag
            textColor = mTextColor
            gravity = Gravity.CENTER
            setPadding(mHorizontalPadding, mVerticalPadding, mHorizontalPadding, mVerticalPadding)
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