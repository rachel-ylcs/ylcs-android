package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.textColor

class TabView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TabLayout(context, attrs, defStyleAttr), TabLayout.OnTabSelectedListener {
    fun interface OnSelectedListener {
        fun onSelected(title: String, position: Int)
    }

    private var textSize: Float = context.resources.getDimension(R.dimen.base)
    private var listener: OnSelectedListener? = null

    init {
        addOnTabSelectedListener(this)
        tabMode = MODE_SCROLLABLE
        tabGravity = GRAVITY_FILL
        isTabIndicatorFullWidth = false
        setSelectedTabIndicatorColor(context.getColor(R.color.steel_blue))
    }

    override fun onTabSelected(tab: Tab) {
        val view = tab.customView as TextView
        view.textColor = context.getColor(R.color.steel_blue)
        listener?.onSelected(view.text.toString(), tab.position)
    }

    override fun onTabUnselected(tab: Tab) {
        val view = tab.customView as TextView
        view.textColor = context.getColor(R.color.black)
    }

    override fun onTabReselected(tab: Tab) { }

    val isEmpty: Boolean get() = tabCount == 0

    fun init(textSize: Float) {
        this.textSize = textSize
    }

    fun addTabEx(title: String) {
        val view = TextView(context)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.textAlignment = TEXT_ALIGNMENT_CENTER
        view.gravity = Gravity.CENTER
        view.textColor = context.getColor(R.color.black)
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        view.text = title
        addTab(newTab().setCustomView(view))
    }

    fun removeTabEx(position: Int) = removeTabAt(position)

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

    fun setOnSelectedListenerEx(listener: OnSelectedListener) {
        this.listener = listener
    }
}