package com.yinlin.rachel.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.luck.picture.lib.photoview.PhotoView
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.model.RachelImageLoader.loadBlack
import com.yinlin.rachel.model.RachelPreview
import com.yinlin.rachel.tool.clearAddAll
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.textSizePx

class Banner @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    class ViewHolder(val view: PhotoView) : RecyclerView.ViewHolder(view)
    class Adapter : RecyclerView.Adapter<ViewHolder>() {
        val items = mutableListOf<RachelPreview>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val pic = PhotoView(parent.context)
            pic.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return ViewHolder(pic)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pic = holder.itemView as PhotoView
            pic.loadBlack(items[position].mImageUrl)
        }
    }

    private val mAdapter = Adapter()
    private val mViewPage = ViewPager2(context)
    private val mText = TextView(context)

    init {
        RachelAttr(context, attrs, R.styleable.Banner).use {
            mText.apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, it.dp(R.styleable.Banner_Banner_TextBottom, 20))
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
                text = "0 / 0"
                textColor = it.color(R.styleable.Banner_Banner_TextColor, R.color.white)
                textSizePx = it.spx(R.styleable.Banner_Banner_TextSize, R.dimen.base)
                paint.isFakeBoldText = it.value(R.styleable.Banner_Banner_TextBold, true)
            }
            mViewPage.offscreenPageLimit = it.value(R.styleable.Banner_Limit, 4).coerceAtLeast(1)
        }

        mViewPage.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mViewPage.adapter = mAdapter
        mViewPage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mText.text = "${position + 1} / ${mAdapter.items.size}"
            }
        })
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addView(mViewPage)
        addView(mText)
    }

    var images: List<RachelPreview>
        get() = mAdapter.items
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            mAdapter.items.clearAddAll(value)
            mAdapter.notifyDataSetChanged()
        }

    @SuppressLint("NotifyDataSetChanged")
    fun setImages(data: List<RachelPreview>, start: Int) {
        mAdapter.items.clearAddAll(data)
        mViewPage.setCurrentItem(start, false)
        mAdapter.notifyDataSetChanged()
    }

    val currentPosition: Int get() = mViewPage.currentItem
    val current: RachelPreview? get() {
        val pos = mViewPage.currentItem
        return if (pos == -1) null else mAdapter.items[pos]
    }
}