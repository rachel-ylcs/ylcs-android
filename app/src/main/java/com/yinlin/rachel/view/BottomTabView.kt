package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.tabs.TabLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.toDP


class BottomTabView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TabLayout(context, attrs, defStyleAttr), TabLayout.OnTabSelectedListener {

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, linePaint)
    }

    override fun onTabSelected(tab: Tab) {
        val item = tab.tag as RachelTab
        val view = tab.customView as ImageView
        view.setImageResource(item.iconActive)
        listener(item)
    }

    override fun onTabUnselected(tab: Tab) {
        val item = tab.tag as RachelTab
        val view = tab.customView as ImageView
        view.setImageResource(item.iconNormal)
    }

    override fun onTabReselected(tab: Tab) { }

    fun setItems(items: Array<RachelTab>, current: Int) {
        val ct = context
        for (item in items) {
            val size = 28.toDP(ct)
            val view = ImageView(ct)
            view.layoutParams = ViewGroup.LayoutParams(size, size)
            view.setImageResource(item.iconNormal)
            addTab(newTab().setCustomView(view).setTag(item))
        }
        selectTab(getTabAt(current))
    }
}