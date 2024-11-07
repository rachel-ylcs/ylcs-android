package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.textColor


class BottomTabView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TabLayout(context, attrs, defStyleAttr), TabLayout.OnTabSelectedListener {
    class TabWrapper(val view: LinearLayout, val item: RachelTab) {
        val text: TextView = view.findViewById(R.id.text)
        val icon: ImageView = view.findViewById(R.id.icon)

        init {
            text.text = item.title
            icon.setImageResource(item.iconNormal)
        }
    }

    fun interface OnSelectedListener {
        fun onSelected(tab: RachelTab)
    }

    private var listener: OnSelectedListener? = null

    init {
        addOnTabSelectedListener(this)
        tabMode = MODE_FIXED
        tabGravity = GRAVITY_FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        getTabAt(0)?.apply {
            setMeasuredDimension(measuredWidth, (this.tag as TabWrapper).view.measuredHeight)
        }
    }

    override fun onTabSelected(tab: Tab) {
        val wrapper = tab.tag as TabWrapper
        wrapper.icon.setImageResource(wrapper.item.iconActive)
        wrapper.text.textColor = context.getColor(R.color.purple)
        listener?.onSelected(wrapper.item)
    }

    override fun onTabUnselected(tab: Tab) {
        val wrapper = tab.tag as TabWrapper
        wrapper.icon.setImageResource(wrapper.item.iconNormal)
        wrapper.text.textColor = context.getColor(R.color.steel_blue)
    }

    override fun onTabReselected(tab: Tab) { }

    val current: Int get() = selectedTabPosition

    fun setItems(items: Array<RachelTab>, current: Int) {
        for (item in items) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_bottom_tab, this, false) as LinearLayout
            addTab(newTab().setCustomView(view).setTag(TabWrapper(view, item)))
        }
        selectTab(getTabAt(current))
    }

    fun setOnSelectedListenerEx(listener: OnSelectedListener) {
        this.listener = listener
    }
}