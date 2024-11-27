package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.model.RachelViewPage
import com.yinlin.rachel.selectorColor
import com.yinlin.rachel.textSizePx

class PageTabView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TabLayout(context, attrs, defStyleAttr) {
    private val textSize: Float

    init {
        RachelAttr(context, attrs, R.styleable.PageTabView).use {
            textSize = it.spx(R.styleable.PageTabView_android_textSize, R.dimen.base)
        }

        tabMode = MODE_SCROLLABLE
        tabGravity = GRAVITY_CENTER
        isTabIndicatorFullWidth = false
    }

    fun bindViewPager(viewPage: ViewPager2, tabs: Array<String>, pages: Array<RachelViewPage<*, *>>) {
        (viewPage.getChildAt(0) as RecyclerView).layoutManager?.isItemPrefetchEnabled = false
        viewPage.adapter = RachelViewPage.Adapter(pages)
        TabLayoutMediator(this, viewPage) { tab, position ->
            val view = TextView(context)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.gravity = Gravity.CENTER
            view.textSizePx = textSize
            view.selectorColor = R.color.selector_text
            view.text = tabs[position]
            tab.customView = view
        }.attach()
    }
}