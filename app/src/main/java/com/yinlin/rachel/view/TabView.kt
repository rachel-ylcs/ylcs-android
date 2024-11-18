package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.textColor
import com.yinlin.rachel.textSizePx

class TabView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TabLayout(context, attrs, defStyleAttr), TabLayout.OnTabSelectedListener {

    private val textSize: Float
    var listener: (Int, String) -> Unit = { _, _ -> }

    init {
        RachelAttr(context, attrs, R.styleable.TabView).use {
            textSize = it.spx(R.styleable.TabView_android_textSize, R.dimen.sm)
        }

        addOnTabSelectedListener(this)
        tabMode = MODE_SCROLLABLE
        tabGravity = GRAVITY_FILL
        isTabIndicatorFullWidth = false
    }

    override fun onTabSelected(tab: Tab) {
        val view = tab.customView as TextView
        view.textColor = context.getColor(R.color.steel_blue)
        listener(tab.position, view.text.toString())
    }

    override fun onTabUnselected(tab: Tab) {
        val view = tab.customView as TextView
        view.textColor = context.getColor(R.color.black)
    }

    override fun onTabReselected(tab: Tab) { }

    val isEmpty: Boolean get() = tabCount == 0

    val current: Int get() = selectedTabPosition

    fun addTabEx(title: String) {
        val view = TextView(context)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.gravity = Gravity.CENTER
        view.textColor = context.getColor(R.color.black)
        view.textSizePx = textSize
        view.text = title
        addTab(newTab().setCustomView(view))
    }

    fun removeTabEx(position: Int) = removeTabAt(position)

    fun clearTabEx() = removeAllTabs()

    inline fun processCurrentTabEx(callback: (view: TextView, title: String, position: Int) -> Unit) {
        val currentPosition = selectedTabPosition
        getTabAt(currentPosition)?.apply {
            val view = this.customView as TextView
            callback(view, view.text.toString(), currentPosition)
        }
    }

    fun selectTabEx(position: Int) = selectTab(getTabAt(position))

    fun scrollToTabEx(position: Int) {
        post { setScrollPosition(position, 0f, true, true) }
    }
}