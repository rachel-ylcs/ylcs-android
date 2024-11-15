package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.toDP


class BottomTabView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TabLayout(context, attrs, defStyleAttr), TabLayout.OnTabSelectedListener {
    class TabWrapper(val layout: LinearLayout, val icon: ImageView, val item: RachelTab)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.gray)
        strokeWidth = 1f.toDP(context)
    }
    var listener: (RachelTab) -> Unit = {}

    init {
        addOnTabSelectedListener(this)
        tabMode = MODE_FIXED
        tabGravity = GRAVITY_FILL
        setSelectedTabIndicator(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        getTabAt(0)?.let {
            val wrapper = it.tag as TabWrapper
            setMeasuredDimension(measuredWidth, wrapper.layout.measuredHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, linePaint)
    }

    override fun onTabSelected(tab: Tab) {
        val wrapper = tab.tag as TabWrapper
        wrapper.icon.setImageResource(wrapper.item.iconActive)
        listener(wrapper.item)
    }

    override fun onTabUnselected(tab: Tab) {
        val wrapper = tab.tag as TabWrapper
        wrapper.icon.setImageResource(wrapper.item.iconNormal)
    }

    override fun onTabReselected(tab: Tab) { }

    fun setItems(items: Array<RachelTab>, current: Int) {
        val inflater = LayoutInflater.from(context)
        for (item in items) {
            val layout = inflater.inflate(R.layout.item_bottom_tab, this, false) as LinearLayout
            val icon = layout.findViewById<ImageView>(R.id.icon)
            icon.setImageResource(item.iconNormal)
            val text = layout.findViewById<TextView>(R.id.text)
            text.text = item.title
            addTab(newTab().setCustomView(layout).setTag(TabWrapper(layout, icon, item)))
        }
        selectTab(getTabAt(current))
    }
}