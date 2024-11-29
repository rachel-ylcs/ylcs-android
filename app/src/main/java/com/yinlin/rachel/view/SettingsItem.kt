package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.textSizePx
import com.yinlin.rachel.tool.toDP
import com.yinlin.rachel.tool.visible

class SettingsItem @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {
    private val iconView = ImageView(context)
    private val textView = TextView(context)
    private val contentView = LinearLayout(context)
    private val arrowView = ImageView(context)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.gray)
        strokeWidth = 1f.toDP(context)
    }
    init {
        iconView.scaleType = ImageView.ScaleType.FIT_XY
        contentView.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        contentView.orientation = HORIZONTAL
        arrowView.setImageResource(R.drawable.icon_expand)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        isClickable = true
        setBackgroundResource(R.drawable.bg_settings_item)

        RachelAttr(context, attrs, R.styleable.SettingsItem).use {
            val iconSize = it.dp(R.styleable.SettingsItem_IconSize, LayoutParams.WRAP_CONTENT)
            iconView.layoutParams = LayoutParams(iconSize, iconSize)
            it.src(R.styleable.SettingsItem_android_src,
                { id -> iconView.setImageResource(id) },
                { color -> iconView.setImageDrawable(ColorDrawable(color)) }
            )
            textView.text = it.value(R.styleable.SettingsItem_android_text, "")
            textView.textColor = it.color(R.styleable.SettingsItem_android_textColor, R.color.black)
            textView.textSizePx = it.spx(R.styleable.SettingsItem_android_textSize, R.dimen.sm)
            textView.paint.isFakeBoldText = it.value(R.styleable.SettingsItem_Bold, true)
            contentView.gravity = it.value(R.styleable.SettingsItem_android_gravity, Gravity.END)
            arrowView.visible = it.value(R.styleable.SettingsItem_HasArrow, true)

            val padding = maxOf(paddingLeft, paddingTop, paddingRight, paddingBottom, 10.toDP(context))
            setPadding(padding, (padding * 1.25).toInt(), padding, (padding * 1.25).toInt())

            val gap = it.dp(R.styleable.SettingsItem_Gap, 10)
            textView.layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(gap, 0, gap * 2,0)
            }
            arrowView.layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(gap, 0, 0, 0)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = width.toFloat()
        val y = height.toFloat()
        canvas.drawLine(0f, y, x, y, linePaint)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val subViews = children.toList()
        removeAllViews()
        addView(iconView)
        addView(textView)
        addView(contentView)
        addView(arrowView)
        for (child in subViews) contentView.addView(child)
    }

    var textColor: Int
        get() = textView.textColor
        set(value) { textView.textColor = value }
}