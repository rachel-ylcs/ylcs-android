package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.rachelClick


class NineGridView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ViewGroup(context, attrs, defStyleAttr) {
    companion object {
        const val MAX_COUNT = 9
    }

    private var mRow = 0
    private var mCol = 0
    private var mImages = emptyList<RachelPreview>()
    private var mGap: Int
    private val mIcon: Int
    var listener: ((Int, RachelPreview) -> Unit)? = null
    var loadImageFunc: (iv: ImageView, path: String) -> Unit = { iv, path -> iv.load(path) }

    init {
        RachelAttr(context, attrs, R.styleable.NineGridView).use {
            mGap = it.dp(R.styleable.NineGridView_Gap, 2)
            mIcon = it.ref(R.styleable.NineGridView_Icon, 0)
        }
    }

    var gap: Int
        get() = mGap
        set(value) {
            mGap = value
            requestLayout()
        }

    var images: List<RachelPreview>
        get() = mImages
        set(value) {
            removeAllViews()
            mImages = value.take(MAX_COUNT)
            val itemCount = mImages.size
            mRow = when (itemCount) {
                in 1..2 -> 1
                in 3..6 -> 2
                in 7..9 -> 3
                else -> 0
            }
            mCol = when (itemCount) {
                1 -> 1
                in 2..4 -> 2
                in 5..9 -> 3
                else -> 0
            }
            if (isSingleVideo) {
                val layout = FrameLayout(context)
                layout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                val view = ImageView(context)
                val item = mImages[0]
                view.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                view.adjustViewBounds = true
                loadImageFunc(view, item.mImageUrl)
                layout.rachelClick { listener?.invoke(0, item) }
                layout.addView(view)
                if (mIcon != 0) {
                    val iconView = ImageView(context)
                    iconView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER
                    }
                    if (item.isVideo) iconView.setImageResource(mIcon)
                    layout.addView(iconView)
                }
                addView(layout)
            }
            else {
                for (index in mImages.indices) {
                    val view = ImageView(context)
                    val item = mImages[index]
                    view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    view.scaleType = ScaleType.CENTER_CROP
                    loadImageFunc(view, item.mImageUrl)
                    view.rachelClick { listener?.invoke(index, item) }
                    addView(view)
                }
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        if (mCol <= 0) setMeasuredDimension(widthSize, 0)
        else {
            val itemCount = mImages.size
            if (isSingleVideo) {
                val child = getChildAt(0)
                child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                setMeasuredDimension(widthSize, child.measuredHeight)
            }
            else {
                val childWidth = (widthSize - (mCol + 1) * mGap) / mCol
                for (i in 0..< itemCount) {
                    val child = getChildAt(i)
                    val spec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
                    child.measure(spec, spec)
                }
                val heightSize = mRow * childWidth + (mRow + 1) * mGap
                setMeasuredDimension(widthSize, heightSize)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (mCol <= 0) return
        val itemCount = mImages.size
        if (isSingleVideo) {
            val child = getChildAt(0)
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
        else {
            for (i in 0 ..< itemCount) {
                val child = getChildAt(i)
                val itemWidth = child.measuredWidth
                val x = i / mCol
                val y = i % mCol
                val left = itemWidth * x + mGap * (x + 1)
                val top = itemWidth * y + mGap * (y + 1)
                child.layout(left, top, left + itemWidth, top + itemWidth)
            }
        }
    }

    private val isSingleVideo: Boolean get() = mImages.size == 1 && mImages[0].isVideo
}